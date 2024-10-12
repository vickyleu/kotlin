/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.isUnconditional
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.objcinterop.isObjCForwardDeclaration
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import java.util.*

// TODO: Similar to IrType.erasedUpperBound from jvm.ir
internal fun IrType.erasure(): IrType {
    if (this !is IrSimpleType) return this

    val upperBound = when (val classifier = classifier) {
        is IrClassSymbol -> classifier.defaultType
        is IrTypeParameterSymbol -> {
            // Pick the (necessarily unique) non-interface upper bound if it exists
            classifier.owner.superTypes.firstOrNull {
                it.classOrNull?.owner?.isInterface == false
            } ?:
            // Otherwise, choose either the first IrClass supertype or recurse.
            // In the first case, all supertypes are interface types and the choice was arbitrary.
            // In the second case, there is only a single supertype.
            classifier.owner.superTypes.first().erasure()
        }
        is IrScriptSymbol -> classifier.unexpectedSymbolKind<IrClassifierSymbol>()
    }

    return upperBound.mergeNullability(this)
}

internal val IrType.erasedUpperBound get() = this.erasure().getClass() ?: error(this.render())

internal class CastsOptimization(val context: Context) : BodyLoweringPass {
    private val not = context.irBuiltIns.booleanNotSymbol

    private sealed class LeafTerm {
        abstract fun invert(): LeafTerm
    }

    private data class ComplexTerm(val expression: IrExpression, val value: Boolean) : LeafTerm() {
        override fun toString() = "[${expression::class.java.simpleName}@0x${System.identityHashCode(expression).toString(16)} is $value]"

        override fun invert() = ComplexTerm(expression, !value)
    }

    private sealed class SimpleTerm(val variable: IrValueDeclaration) : LeafTerm() {
        class Is(value: IrValueDeclaration, val irClass: IrClass) : SimpleTerm(value) {
            override fun toString() = "${variable.name} is ${irClass.defaultType.render()}"

            override fun equals(other: Any?): Boolean {
                if (other === this) return true
                if (other !is Is) return false
                return variable == other.variable && irClass == other.irClass
            }

            override fun invert() = IsNot(variable, irClass)
        }

        class IsNot(value: IrValueDeclaration, val irClass: IrClass) : SimpleTerm(value) {
            override fun toString() = "${variable.name} !is ${irClass.defaultType.render()}"

            override fun equals(other: Any?): Boolean {
                if (other === this) return true
                if (other !is IsNot) return false
                return variable == other.variable && irClass == other.irClass
            }

            override fun invert() = Is(variable, irClass)
        }

        class IsNull(value: IrValueDeclaration) : SimpleTerm(value) {
            override fun toString() = "${variable.name} == null"

            override fun equals(other: Any?): Boolean {
                if (other === this) return true
                if (other !is IsNull) return false
                return variable == other.variable
            }

            override fun invert() = IsNotNull(variable)
        }

        class IsNotNull(value: IrValueDeclaration) : SimpleTerm(value) {
            override fun toString() = "${variable.name} != null"

            override fun equals(other: Any?): Boolean {
                if (other === this) return true
                if (other !is IsNotNull) return false
                return variable == other.variable
            }

            override fun invert() = IsNull(variable)
        }
    }

    private class Disjunction(val terms: List<LeafTerm>) {
        init {
            require(terms.isNotEmpty())
        }

        override fun toString() = when (terms.size) {
            1 -> terms.first().toString()
            else -> terms.joinToString(" | ") { "(${it})" }
        }

        // (a | b) & (a) = a
        infix fun followsFrom(other: Disjunction): Boolean {
            for (term in other.terms) {
                if (!terms.contains(term))
                    return false
            }
            return true
        }
    }

    private sealed class Predicate {
        //data object True : Predicate()
        data object False : Predicate()
        data object Empty : Predicate()
    }

    private class Conjunction(val terms: List<Disjunction>) : Predicate() {
        init {
            require(terms.isNotEmpty())
        }

        override fun toString() = when (terms.size) {
            1 -> terms.first().toString()
            else -> terms.joinToString(separator = " & ") { "($it)" }
        }
    }

    private object Predicates {
        fun disjunctionOf(vararg terms: LeafTerm): Predicate {
            return disjunctionOf(terms.asList(), optimize = false)
        }

        fun disjunctionOf(terms: List<LeafTerm>, optimize: Boolean = true): Predicate {
            val optimizedTerms = if (optimize) optimizeTerms(terms) else terms
            return if (optimizedTerms.isEmpty()) Predicate.Empty else Conjunction(listOf(Disjunction(optimizedTerms)))
        }

        fun isSubtypeOf(value: IrValueDeclaration, type: IrType): Predicate {
            val valueIsNullable = value.type.isNullable()
            val typeIsNullable = type.isNullable()
            val dstClass = type.erasedUpperBound
            val isSuperClassCast = value.type.classifierOrNull !is IrTypeParameterSymbol // Due to unsafe casts, see unchecked_cast8.kt as an example.
                    && value.type.isSubtypeOfClass(dstClass.symbol)
            return when {
                isSuperClassCast -> {
                    if (valueIsNullable && !typeIsNullable) // (value: A?) is A = value != null
                        disjunctionOf(SimpleTerm.IsNotNull(value))
                    else Predicate.Empty
                }
                else -> {
                    if (valueIsNullable && typeIsNullable) // (value: A?) is B? = value == null || value is B
                        disjunctionOf(SimpleTerm.IsNull(value), SimpleTerm.Is(value, dstClass))
                    else
                        disjunctionOf(SimpleTerm.Is(value, dstClass))
                }
            }
        }

        private fun optimizeTerms(leafTerms: List<LeafTerm>): List<LeafTerm> {
            val simpleTerms = mutableListOf<SimpleTerm>()
            val complexTerms = mutableListOf<ComplexTerm>()
            for (term in leafTerms) {
                if (term is SimpleTerm)
                    simpleTerms.add(term)
                else
                    complexTerms.add(term as ComplexTerm)
            }
            val result = mutableListOf<LeafTerm>()
            val groupedSimpleTerms = simpleTerms.groupBy { it.variable }
            groupedSimpleTerms.forEach { (variable, terms) ->
                val classes = mutableMapOf<IrClass, Boolean>()
                var isNull: Boolean? = null
                for (term in terms) when (term) {
                    is SimpleTerm.Is -> {
                        val isSubtype = classes.getOrPut(term.irClass) { true }
                        if (!isSubtype) {
                            // x !is T || x is T => always true
                            return emptyList()
                        }
                    }
                    is SimpleTerm.IsNot -> {
                        val isSubtype = classes.getOrPut(term.irClass) { false }
                        if (isSubtype) {
                            // x is T || x !is T => always true
                            return emptyList()
                        }
                    }
                    is SimpleTerm.IsNull -> {
                        if (isNull == false) {
                            // x != null || x == null => always true
                            return emptyList()
                        }
                        isNull = true
                    }
                    is SimpleTerm.IsNotNull -> {
                        if (isNull == true) {
                            // x == null || x != null => always true
                            return emptyList()
                        }
                        isNull = false
                    }
                }
                if (isNull != null)
                    result.add(if (isNull) SimpleTerm.IsNull(variable) else SimpleTerm.IsNotNull(variable))
                classes.forEach { (irClass, isSubtype) ->
                    result.add(if (isSubtype) SimpleTerm.Is(variable, irClass) else SimpleTerm.IsNot(variable, irClass))
                }
            }

            val groupedComplexTerms = complexTerms.groupBy { it.expression }
            groupedComplexTerms.forEach { (expression, terms) ->
                var value: Boolean? = null
                for (term in terms) {
                    if (value != null && value != term.value) {
                        // expression && !expression => always true
                        return emptyList()
                    }
                    value = term.value
                }
                result.add(ComplexTerm(expression, value!!))
            }

            return result
        }
    }

    private data class PredicatePair(val ifTrue: Predicate, val ifFalse: Predicate) {
        fun invert() = PredicatePair(ifFalse, ifTrue)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container.fileOrNull?.path?.endsWith("z3.kt") != true) return

        irBody.accept(object : IrElementVisitor<Predicate, Predicate> {
            fun IrExpression.matchAndAnd(): Pair<IrExpression, IrExpression>? = when {
                // a && b == if (a) b else false
                (this as? IrWhen)?.branches?.size == 2
                        && this.branches[1].isUnconditional()
                        && (this.branches[1].result as? IrConst)?.value == false
                -> Pair(this.branches[0].condition, this.branches[0].result)
                else -> null
            }

            fun IrExpression.matchOrOr(): Pair<IrExpression, IrExpression>? = when {
                // a || b == if (a) true else b
                (this as? IrWhen)?.branches?.size == 2
                        && this.branches[1].isUnconditional()
                        && (this.branches[0].result as? IrConst)?.value == true
                -> Pair(this.branches[0].condition, this.branches[1].result)
                else -> null
            }

            fun IrExpression.matchNot(): IrExpression? = when {
                (this as? IrCall)?.symbol == not -> this.dispatchReceiver!!
                else -> null
            }

// TODO: Do we need to handle this?
//            fun IrExpression.matchXor(): Pair<IrExpression, IrExpression>? = when {
//                (this as? IrCall)?.symbol
//            }


            fun orPredicates(leftPredicate: Predicate, rightPredicate: Predicate): Predicate = when {
//                leftPredicate == Predicate.True || rightPredicate == Predicate.True -> {
//                    Predicate.True
//                }
                leftPredicate == Predicate.False -> {
                    rightPredicate
                }
                rightPredicate == Predicate.False -> {
                    leftPredicate
                }
                leftPredicate == Predicate.Empty -> {
                    rightPredicate
                }
                rightPredicate == Predicate.Empty -> {
                    leftPredicate
                }
                else -> {
                    leftPredicate as Conjunction
                    rightPredicate as Conjunction
                    when {
                        leftPredicate.terms.size <= 1 && rightPredicate.terms.size <= 1 -> {
                            val simpleTerms = mutableListOf<LeafTerm>()
                            if (leftPredicate.terms.size == 1)
                                simpleTerms.addAll(leftPredicate.terms[0].terms)
                            if (rightPredicate.terms.size == 1)
                                simpleTerms.addAll(rightPredicate.terms[0].terms)
                            Predicates.disjunctionOf(simpleTerms)
                        }
                        rightPredicate.terms.size == 1 -> {
                            // a | (b & c) = (a | b) & (a | c)
                            // a = right.terms[0], b = left.terms[0], c = left.terms[1:]
                            andPredicates(
                                    Predicates.disjunctionOf(leftPredicate.terms.first().terms + rightPredicate.terms.first().terms),
                                    orPredicates(rightPredicate, Conjunction(leftPredicate.terms.drop(1)))
                            )
                        }
                        else -> {
                            // a | (b & c) = (a | b) & (a | c)
                            // a = left, b = right.terms[0], c = right.terms[1:]
                            andPredicates(
                                    orPredicates(leftPredicate, Conjunction(listOf(rightPredicate.terms.first()))),
                                    orPredicates(leftPredicate, Conjunction(rightPredicate.terms.drop(1)))
                            )
                        }
                    }
                }
            }.also {
                println("OR: $leftPredicate")
                println("    $rightPredicate")
                println(" =  $it")
//                val t = IllegalStateException()
//                println(t.stackTraceToString())
            }

            fun andPredicates(leftPredicate: Predicate, rightPredicate: Predicate): Predicate {
                if (leftPredicate == Predicate.False || rightPredicate == Predicate.False)
                    return Predicate.False
                if (/*leftPredicate == Predicate.True || */leftPredicate == Predicate.Empty)
                    return rightPredicate
                if (/*rightPredicate == Predicate.True || */rightPredicate == Predicate.Empty)
                    return leftPredicate

                leftPredicate as Conjunction
                rightPredicate as Conjunction
                val leftTerms = mutableListOf<Disjunction>()
                for (term in leftPredicate.terms) {
                    if (rightPredicate.terms.any { term followsFrom it })
                        continue
                    val falseLeafIndices = BitSet()
                    term.terms.forEachIndexed { index, leafTerm ->
                        val invertedLeafTerm = leafTerm.invert()
                        if (rightPredicate.terms.any { it.terms.singleOrNull() == invertedLeafTerm })
                            falseLeafIndices.set(index)
                    }
                    when (falseLeafIndices.size()) {
                        0 -> leftTerms.add(term)
                        term.terms.size -> return Predicate.False
                        else -> leftTerms.add(Disjunction(term.terms.filterIndexed { index, _ -> !falseLeafIndices.get(index) }))
                    }
                }
                val rightTerms = mutableListOf<Disjunction>()
                for (term in rightPredicate.terms) {
                    if (leftTerms.any { term followsFrom it })
                        continue
                    val falseLeafIndices = BitSet()
                    term.terms.forEachIndexed { index, leafTerm ->
                        val invertedLeafTerm = leafTerm.invert()
                        if (leftTerms.any { it.terms.singleOrNull() == invertedLeafTerm })
                            falseLeafIndices.set(index)
                    }
                    when (falseLeafIndices.size()) {
                        0 -> rightTerms.add(term)
                        term.terms.size -> return Predicate.False
                        else -> rightTerms.add(Disjunction(term.terms.filterIndexed { index, _ -> !falseLeafIndices.get(index) }))
                    }
                }
//                val leftTerms = leftPredicate.terms.filter { leftTerm ->
//                    !rightPredicate.terms.any { leftTerm followsFrom it }
//                }
//                val rightTerms = rightPredicate.terms.filter { rightTerm ->
//                    !leftTerms.any { rightTerm followsFrom it }
//                }


                // (a | b) & (!a) = b & !a
                // (a1 | a2 | b) & !a1 & !a2 = (a1 | b) & !a1 & !a2 = b & !a1 & !a2

                return Conjunction(leftTerms + rightTerms)
            }

            fun invertPredicate(predicate: Predicate): Predicate = when (predicate) {
                //Predicate.True -> Predicate.False
                Predicate.False -> Predicate.Empty
                Predicate.Empty -> Predicate.False
                is Conjunction -> when {
                    predicate.terms.size == 1 -> {
                        Conjunction(predicate.terms.first().terms.map { Disjunction(listOf(it.invert())) })
                    }
                    else -> {
                        orPredicates(
                                invertPredicate(Conjunction(listOf(predicate.terms.first()))),
                                invertPredicate(Conjunction(predicate.terms.drop(1)))
                        )
                    }
                }
            }

            fun buildPredicate(expression: IrExpression, predicate: Predicate): PredicatePair {
                val matchResultAndAnd = expression.matchAndAnd()
                if (matchResultAndAnd != null) {
                    val (left, right) = matchResultAndAnd
                    val leftPredicatePair = buildPredicate(left, predicate)
                    val rightPredicatePair = buildPredicate(right, leftPredicatePair.ifTrue)
                    return PredicatePair(
                            ifTrue = rightPredicatePair.ifTrue,
                            ifFalse = orPredicates(leftPredicatePair.ifFalse, rightPredicatePair.ifFalse)
                    )
                }
                val matchResultOrOr = expression.matchOrOr()
                if (matchResultOrOr != null) {
                    val (left, right) = matchResultOrOr
                    val leftPredicatePair = buildPredicate(left, predicate)
//                    println("leftPredicatePair:")
//                    println("    ${leftPredicatePair.ifTrue}")
//                    println("    ${leftPredicatePair.ifFalse}")
                    val rightPredicatePair = buildPredicate(right, leftPredicatePair.ifFalse)
//                    println("rightPredicatePair:")
//                    println("    ${rightPredicatePair.ifTrue}")
//                    println("    ${rightPredicatePair.ifFalse}")
                    return PredicatePair(
                            ifTrue = orPredicates(leftPredicatePair.ifTrue, rightPredicatePair.ifTrue),
                            ifFalse = rightPredicatePair.ifFalse
                    )
                }
                val matchResultNot = expression.matchNot()
                if (matchResultNot != null) {
                    return buildPredicate(matchResultNot, predicate).invert()
                }

                // TODO: == null, === null

                if ((expression as? IrConst)?.value == true) {
                    return PredicatePair(ifTrue = predicate, ifFalse = Predicate.False)
                }
                if ((expression as? IrConst)?.value == false) {
                    return PredicatePair(ifTrue = Predicate.False, ifFalse = predicate)
                }
                // if (x as? String != null) ...  =  if (x is String) ...
                if (expression is IrTypeOperatorCall) {
                    val argument = expression.argument
                    if (argument is IrGetValue) {
                        val rootValue = argument.getRootValue()
                        if (expression.operator == IrTypeOperator.INSTANCEOF || expression.operator == IrTypeOperator.NOT_INSTANCEOF) {
                            val isSubtypeOfPredicate = Predicates.isSubtypeOf(rootValue, expression.typeOperand)
                            val result = PredicatePair(
                                    ifTrue = andPredicates(predicate, isSubtypeOfPredicate),
                                    ifFalse = andPredicates(predicate, invertPredicate(isSubtypeOfPredicate))
                            )
                            return if (expression.operator == IrTypeOperator.INSTANCEOF)
                                result
                            else
                                result.invert()
                        }
                    }
                }

                val result = expression.accept(this, predicate)
                return PredicatePair(
                        ifTrue = andPredicates(Predicates.disjunctionOf(ComplexTerm(expression, value = true)), result),
                        ifFalse = andPredicates(Predicates.disjunctionOf(ComplexTerm(expression, value = false)), result)
                )
            }

            private fun IrElement.getImmediateChildren(): List<IrElement> {
                val result = mutableListOf<IrElement>()
                acceptChildrenVoid(object : IrElementVisitorVoid {
                    override fun visitElement(element: IrElement) {
                        result.add(element)
                        // Do not recurse.
                    }
                })
                return result
            }

            override fun visitElement(element: IrElement, data: Predicate): Predicate {
                val children = element.getImmediateChildren()
                var predicate = data
                for (child in children)
                    predicate = child.accept(this, predicate)
                return predicate
            }

            fun IrTypeOperatorCall.isCast() = operator == IrTypeOperator.CAST || operator == IrTypeOperator.IMPLICIT_CAST

            fun IrExpression.unwrapCasts(): IrExpression {
                var result = this
                while ((result as? IrTypeOperatorCall)?.isCast() == true)
                    result = result.argument
                return result
            }

            fun IrGetValue.getRootValue(): IrValueDeclaration {
                val value = this.symbol.owner
                if (value is IrValueParameter || (value as IrVariable).isVar)
                    return value
                val initializerRootValue = (value.initializer as? IrGetValue)?.getRootValue()
                return if (initializerRootValue == null || (initializerRootValue as? IrVariable)?.isVar == true)
                    value
                else initializerRootValue
            }

            override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Predicate): Predicate {
                /*
                  TYPE_OP type=<root>.A origin=IMPLICIT_CAST typeOperand=<root>.A
                    TYPE_OP type=<root>.A? origin=CAST typeOperand=<root>.A?
                      TYPE_OP type=kotlin.Any origin=IMPLICIT_CAST typeOperand=kotlin.Any
                        GET_VAR 'x: kotlin.Any declared in <root>.foo' type=kotlin.Any origin=null
                 */
                val result = expression.argument.accept(this, data)
                if (expression.isCast()) {
                    println("ZZZ: ${expression.dump()}")
                    println("    $result")
                    println()
                    val argument = expression.unwrapCasts()
                    if (argument is IrGetValue)
                        return andPredicates(result, Predicates.isSubtypeOf(argument.getRootValue(), expression.typeOperand))
                }
                return result
            }

            override fun visitWhen(expression: IrWhen, data: Predicate): Predicate {
                var predicate = data
                var result: Predicate = Predicate.Empty
                for (branch in expression.branches) {
                    val branchPredicatePair = buildPredicate(branch.condition, predicate)
                    println("QXX: ${branch.condition.dump()}")
                    println("    ${branchPredicatePair.ifTrue}")
                    println("    ${branchPredicatePair.ifFalse}")
                    println("    $result")
                    println()
                    result = orPredicates(result, branch.result.accept(this, branchPredicatePair.ifTrue))
                    predicate = branchPredicatePair.ifFalse
                }
                println("QXX")
                println("    $result")
                println("    $predicate")
                result = orPredicates(result, predicate)
                println("    $result")
                println()

                return result
            }

        }, Predicate.Empty)
    }
}

internal class TypeOperatorLowering(val context: CommonBackendContext) : FileLoweringPass, IrBuildingTransformer(context) {

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(this, null)
    }

    private fun effectiveCheckType(type: IrType) : IrType {
        val erasedType = type.erasure()
        return if (erasedType.classOrNull?.owner?.isObjCForwardDeclaration() == true) {
            context.irBuiltIns.anyType.mergeNullability(erasedType)
        } else {
            erasedType
        }
    }

    private fun lowerCast(expression: IrTypeOperatorCall): IrExpression {
        builder.at(expression)
        val typeOperand = effectiveCheckType(expression.typeOperand)
        return if (typeOperand == expression.typeOperand) {
            expression
        } else {
            builder.irAs(expression.argument, typeOperand)
        }
    }

    private fun lowerSafeCast(expression: IrTypeOperatorCall): IrExpression {
        val typeOperand = effectiveCheckType(expression.typeOperand)

        return builder.irBlock(expression) {
            +irLetS(expression.argument) { variable ->
                irIfThenElse(expression.type,
                        condition = irIs(irGet(variable.owner), typeOperand),
                        thenPart = irImplicitCast(irGet(variable.owner), typeOperand),
                        elsePart = irNull())
            }
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        expression.transformChildrenVoid(this)

        return when (expression.operator) {
            IrTypeOperator.SAFE_CAST -> lowerSafeCast(expression)
            IrTypeOperator.CAST -> lowerCast(expression)
            else -> expression
        }
    }
}

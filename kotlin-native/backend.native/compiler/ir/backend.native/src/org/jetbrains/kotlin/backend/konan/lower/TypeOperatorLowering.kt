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
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.*
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
    private val eqeq = context.irBuiltIns.eqeqSymbol
    private val eqeqeq = context.irBuiltIns.eqeqeqSymbol
    private val ieee754EqualsSymbols: Set<IrSimpleFunctionSymbol> =
            context.irBuiltIns.ieee754equalsFunByOperandType.values.toSet()

    private sealed class LeafTerm

    private data class ComplexTerm(val expression: IrExpression) : LeafTerm() {
        override fun toString() = "[${expression::class.java.simpleName}@0x${System.identityHashCode(expression).toString(16)}]"
    }

    private sealed class SimpleTerm(val variable: IrValueDeclaration) : LeafTerm() {
        class Is(value: IrValueDeclaration, val irClass: IrClass) : SimpleTerm(value) {
            override fun toString() = "${variable.name} is ${irClass.defaultType.render()}"

            override fun hashCode(): Int {
                return variable.hashCode() * 31 + irClass.hashCode()
            }

            override fun equals(other: Any?): Boolean {
                if (other === this) return true
                if (other !is Is) return false
                return variable == other.variable && irClass == other.irClass
            }
        }

        class IsNull(value: IrValueDeclaration) : SimpleTerm(value) {
            override fun toString() = "${variable.name} == null"

            override fun hashCode(): Int {
                return variable.hashCode()
            }

            override fun equals(other: Any?): Boolean {
                if (other === this) return true
                if (other !is IsNull) return false
                return variable == other.variable
            }
        }
    }

    private class Conjunction(val terms: List<LeafTerm>) {
        init {
            require(terms.isNotEmpty())
        }

        override fun toString() = when (terms.size) {
            1 -> terms.first().toString()
            else -> terms.joinToString("") { "(${it})" }
        }

        override fun hashCode(): Int {
            return terms.sumOf { it.hashCode() }
        }

        override fun equals(other: Any?): Boolean {
            val otherTerms = (other as? Conjunction)?.terms ?: return false
            if (terms.size != otherTerms.size) return false
            val set = terms.toSet()
            return otherTerms.all { it in set }
        }
    }

    private class Predicate(val terms: List<Conjunction>, val inverted: Boolean) {
        override fun toString(): String {
            if (this === Zero) return "0"
            if (this === One) return "1"
            val termsFormatted = when (terms.size) {
                1 -> terms.first().toString()
                else -> terms.joinToString(separator = " + ") { "($it)" }
            }
            return if (inverted) "1 + ($termsFormatted)" else termsFormatted
        }

        companion object {
            val Zero = Predicate(emptyList(), inverted = false)
            val One = Predicate(emptyList(), inverted = true)

            fun create(terms: List<Conjunction>, inverted: Boolean): Predicate {
                val uniqueTerms = mutableSetOf<Conjunction>()
                for (term in terms) {
                    if (!uniqueTerms.add(term))
                        uniqueTerms.remove(term)
                }
                return when {
                    uniqueTerms.isEmpty() -> if (inverted) One else Zero
                    else -> {
                        Predicate(uniqueTerms.toList(), inverted)
                    }
                }
            }

            fun createSingleConjunction(terms: List<LeafTerm>, inverted: Boolean) = when {
                terms.isEmpty() -> if (inverted) One else Zero
                else -> Predicate(listOf(Conjunction(terms)), inverted)
            }
        }
    }

    private object Predicates {
        fun conjunctionOf(vararg terms: LeafTerm) =
                conjunctionOf(terms.asList(), inverted = false, optimize = false)

        fun invertedConjunctionOf(vararg terms: LeafTerm) =
                conjunctionOf(terms.asList(), inverted = true, optimize = false)

        fun conjunctionOf(terms: List<LeafTerm>, inverted: Boolean, optimize: Boolean = true) =
                Predicate.createSingleConjunction(if (optimize) optimizeTerms(terms) else terms, inverted)

        fun isSubtypeOf(value: IrValueDeclaration, type: IrType): Predicate {
            val valueIsNullable = value.type.isNullable()
            val typeIsNullable = type.isNullable()
            val dstClass = type.erasedUpperBound
            val isSuperClassCast = value.type.classifierOrNull !is IrTypeParameterSymbol // Due to unsafe casts, see unchecked_cast8.kt as an example.
                    && value.type.isSubtypeOfClass(dstClass.symbol)
            return when {
                isSuperClassCast -> {
                    if (valueIsNullable && !typeIsNullable) // (value: A?) is A = value != null
                        invertedConjunctionOf(SimpleTerm.IsNull(value))
                    else Predicate.One
                }
                else -> {
                    if (valueIsNullable && typeIsNullable) // (value: A?) is B? = value == null || value is B
                        Predicate.create(listOf(
                                Conjunction(listOf(SimpleTerm.IsNull(value))),
                                Conjunction(listOf(SimpleTerm.Is(value, dstClass))),
                        ), inverted = false)
                    else
                        conjunctionOf(SimpleTerm.Is(value, dstClass))
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
                val classes = mutableSetOf<IrClass>()
                var isNull = false
                for (term in terms) when (term) {
                    is SimpleTerm.Is -> classes.add(term.irClass)
                    is SimpleTerm.IsNull -> isNull = true
                }
                if (isNull && classes.isNotEmpty()) // x == null && x is A -> always false
                    return emptyList()
                if (isNull)
                    result.add(SimpleTerm.IsNull(variable))
                classes.forEach { irClass ->
                    result.add(SimpleTerm.Is(variable, irClass))
                }
            }

            result.addAll(complexTerms.distinct())

            return result
        }
    }

    private data class BooleanPredicate(val ifTrue: Predicate, val ifFalse: Predicate) {
        fun invert() = BooleanPredicate(ifFalse, ifTrue)
    }

    private data class NullablePredicate(val ifNull: Predicate, val ifNotNull: Predicate) {
        fun invert() = NullablePredicate(ifNotNull, ifNull)
    }

    private fun findBlocks(irBody: IrBody, container: IrDeclaration) {
        var hasBlocks = false
        irBody.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            fun IrExpression.isNullConst() = this is IrConst && this.value == null

            fun IrSimpleFunctionSymbol.isEqualityOperator() = this == eqeq || this == eqeqeq || this in ieee754EqualsSymbols

            fun IrExpression.matchEquality(): Pair<IrExpression, IrExpression>? = when {
                (this as? IrCall)?.symbol?.isEqualityOperator() == true ->
                    Pair(this.getValueArgument(0)!!, this.getValueArgument(1)!!)
                else -> null
            }

            fun IrExpression.matchSafeCall(): Triple<IrValueDeclaration, IrType?, IrExpression>? {
                val statements = (this as? IrBlock)?.statements?.takeIf { it.size == 2 } ?: return null
                val safeReceiver = statements[0] as? IrVariable ?: return null
                val initializer = safeReceiver.initializer
                val variableGetter: IrGetValue
                val type: IrType?
                when (initializer) {
                    is IrGetValue -> {
                        variableGetter = initializer
                        type = null
                    }
                    is IrTypeOperatorCall -> {
                        if (initializer.operator != IrTypeOperator.SAFE_CAST)
                            return null
                        variableGetter = initializer.argument as? IrGetValue ?: return null
                        type = initializer.typeOperand
                    }
                    else -> return null
                }
                val safeCallResultWhen = (statements[1] as? IrWhen)?.takeIf { it.branches.size == 2 } ?: return null
                val equalityMatchResult = safeCallResultWhen.branches[0].condition.matchEquality() ?: return null
                if ((equalityMatchResult.first as? IrGetValue)?.symbol?.owner != safeReceiver
                        || !equalityMatchResult.second.isNullConst()
                        || !safeCallResultWhen.branches[0].result.isNullConst())
                    return null
                if (!safeCallResultWhen.branches[1].isUnconditional())
                    return null

                return Triple(variableGetter.symbol.owner, type, safeCallResultWhen.branches[1].result)
            }

            override fun visitCall(expression: IrCall) {
                expression.acceptChildrenVoid(this)

                if (expression.symbol == eqeq) {
                    val left = expression.getValueArgument(0)!!
                    val right = expression.getValueArgument(1)!!
                    val leftIsNullConst = left.isNullConst()
                    val rightIsNullConst = right.isNullConst()
                    if ((leftIsNullConst || !left.type.isNullable()) && right.type.isNullable()) {
                        if (right is IrBlock && right !is IrReturnableBlock && right.origin != LoweredStatementOrigins.INLINE_ARGS_CONTAINER
                                && right.matchSafeCall() == null
                        ) {
                            hasBlocks = true
                            println(expression.dump())
                            println()
                        }
                    }
                    if ((rightIsNullConst || !right.type.isNullable()) && left.type.isNullable()) {
                        if (left is IrBlock && left !is IrReturnableBlock && left.origin != LoweredStatementOrigins.INLINE_ARGS_CONTAINER
                                && left.matchSafeCall() == null
                        ) {
                            hasBlocks = true
                            println(expression.dump())
                            println()
                        }
                    }
                }
            }
        })
        if (hasBlocks) {
            println("ZZZ: ${container.render()}")
            println()
            println()
        }
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
//        if (container.fileOrNull?.path?.endsWith("z.kt") != true) return
//        val debugOutput = true
        val debugOutput = container.fileOrNull?.path?.endsWith("collections/Arrays.kt") == true

        irBody.accept(object : IrElementVisitor<Predicate, Predicate> {
            val variablePredicates = mutableMapOf<IrValueDeclaration, BooleanPredicate>()

            fun IrExpression.isNullConst() = this is IrConst && this.value == null

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

            fun IrSimpleFunctionSymbol.isEqualityOperator() = this == eqeq || this == eqeqeq || this in ieee754EqualsSymbols

            fun IrExpression.matchEquality(): Pair<IrExpression, IrExpression>? = when {
                (this as? IrCall)?.symbol?.isEqualityOperator() == true ->
                    Pair(this.getValueArgument(0)!!, this.getValueArgument(1)!!)
                else -> null
            }

            fun IrExpression.matchSafeCall(): Triple<IrValueDeclaration, IrType?, IrExpression>? {
                val statements = (this as? IrBlock)?.statements?.takeIf { it.size == 2 } ?: return null
                val safeReceiver = statements[0] as? IrVariable ?: return null
                val initializer = safeReceiver.initializer
                val variableGetter: IrGetValue
                val type: IrType?
                when (initializer) {
                    is IrGetValue -> {
                        variableGetter = initializer
                        type = null
                    }
                    is IrTypeOperatorCall -> {
                        if (initializer.operator != IrTypeOperator.SAFE_CAST)
                            return null
                        variableGetter = initializer.argument as? IrGetValue ?: return null
                        type = initializer.typeOperand
                    }
                    else -> return null
                }
                val safeCallResultWhen = (statements[1] as? IrWhen)?.takeIf { it.branches.size == 2 } ?: return null
                val equalityMatchResult = safeCallResultWhen.branches[0].condition.matchEquality() ?: return null
                if ((equalityMatchResult.first as? IrGetValue)?.symbol?.owner != safeReceiver
                        || !equalityMatchResult.second.isNullConst()
                        || !safeCallResultWhen.branches[0].result.isNullConst())
                    return null
                if (!safeCallResultWhen.branches[1].isUnconditional())
                    return null

                return Triple(variableGetter.getRootValue(), type, safeCallResultWhen.branches[1].result)
            }

            fun orPredicates(leftPredicate: Predicate, rightPredicate: Predicate): Predicate {
                val result = invertPredicate(andPredicates(invertPredicate(leftPredicate), invertPredicate(rightPredicate)))
                return result.also {
                    if (debugOutput) {
                        println("OR: $leftPredicate")
                        println("    $rightPredicate")
                        println(" =  $it")
            //                val t = IllegalStateException()
            //                println(t.stackTraceToString())
                    }
                }
            }

            fun andPredicates(leftPredicate: Predicate, rightPredicate: Predicate): Predicate {
                val terms = mutableListOf<Conjunction>()
                if (leftPredicate.inverted)
                    terms.addAll(rightPredicate.terms)
                if (rightPredicate.inverted)
                    terms.addAll(leftPredicate.terms)
                for (leftTerm in leftPredicate.terms) {
                    for (rightTerm in rightPredicate.terms) {
                        val andResult = Predicates.conjunctionOf(leftTerm.terms + rightTerm.terms, inverted = false)
                        if (andResult != Predicate.Zero)
                            terms.add(Conjunction(andResult.terms.singleOrNull()!!.terms))
                    }
                }

                return Predicate.create(terms, inverted = leftPredicate.inverted && rightPredicate.inverted)
            }

            fun invertPredicate(predicate: Predicate): Predicate = Predicate.create(predicate.terms, !predicate.inverted)

            fun buildBooleanPredicate(expression: IrExpression, predicate: Predicate): BooleanPredicate {
                val matchResultAndAnd = expression.matchAndAnd()
                if (matchResultAndAnd != null) {
                    val (left, right) = matchResultAndAnd
                    val leftBooleanPredicate = buildBooleanPredicate(left, predicate)
                    val rightBooleanPredicate = buildBooleanPredicate(right, leftBooleanPredicate.ifTrue)
                    return BooleanPredicate(
                            ifTrue = rightBooleanPredicate.ifTrue,
                            ifFalse = orPredicates(leftBooleanPredicate.ifFalse, rightBooleanPredicate.ifFalse)
                    )
                }
                val matchResultOrOr = expression.matchOrOr()
                if (matchResultOrOr != null) {
                    val (left, right) = matchResultOrOr
                    val leftBooleanPredicate = buildBooleanPredicate(left, predicate)
                    val rightBooleanPredicate = buildBooleanPredicate(right, leftBooleanPredicate.ifFalse)
                    return BooleanPredicate(
                            ifTrue = orPredicates(leftBooleanPredicate.ifTrue, rightBooleanPredicate.ifTrue),
                            ifFalse = rightBooleanPredicate.ifFalse
                    )
                }
                val matchResultNot = expression.matchNot()
                if (matchResultNot != null) {
                    return buildBooleanPredicate(matchResultNot, predicate).invert()
                }

                // if (x as? A != null) ...  =  if (x is A) ...
                // if ((x as? A)?.y == ..)
                val matchResultEquality = expression.matchEquality()
                if (matchResultEquality != null) {
                    val (left, right) = matchResultEquality
                    val leftIsNullConst = left.isNullConst()
                    val rightIsNullConst = right.isNullConst()
                    return if ((leftIsNullConst || !left.type.isNullable()) && right.type.isNullable()) {
                        val predicateAfterLeft = if (leftIsNullConst) predicate else visitElement(left, predicate)
                        val nullablePredicate = buildNullablePredicate(right, predicateAfterLeft)
                        if (nullablePredicate == null) {
                            val result = visitElement(right, predicateAfterLeft)
                            BooleanPredicate(ifTrue = result, ifFalse = result)
                        } else if (leftIsNullConst) {
                            BooleanPredicate(
                                    ifTrue = nullablePredicate.ifNull,
                                    ifFalse = nullablePredicate.ifNotNull
                            )
                        } else {
                            BooleanPredicate(
                                    ifTrue = nullablePredicate.ifNotNull,
                                    ifFalse = nullablePredicate.ifNull
                            )
                        }
                    } else if ((rightIsNullConst || !right.type.isNullable()) && left.type.isNullable()) {
                        val nullablePredicate = buildNullablePredicate(left, predicate)
                        return if (nullablePredicate == null) {
                            val result = visitElement(expression, predicate)
                            BooleanPredicate(ifTrue = result, ifFalse = result)
                        } else if (rightIsNullConst) {
                            BooleanPredicate(
                                    ifTrue = nullablePredicate.ifNull,
                                    ifFalse = nullablePredicate.ifNotNull
                            )
                        } else {
                            BooleanPredicate(
                                    ifTrue = visitElement(right, nullablePredicate.ifNotNull),
                                    ifFalse = visitElement(right, nullablePredicate.ifNull)
                            )
                        }
                    } else {
                        val result = expression.accept(this, predicate)
                        return BooleanPredicate(
                                ifTrue = andPredicates(Predicates.conjunctionOf(ComplexTerm(expression)), result),
                                ifFalse = andPredicates(Predicates.invertedConjunctionOf(ComplexTerm(expression)), result)
                        )
                    }
                }

                if ((expression as? IrConst)?.value == true) {
                    return BooleanPredicate(ifTrue = predicate, ifFalse = Predicate.Zero)
                }
                if ((expression as? IrConst)?.value == false) {
                    return BooleanPredicate(ifTrue = Predicate.Zero, ifFalse = predicate)
                }
                if (expression is IrTypeOperatorCall) {
                    val argument = expression.argument
                    if (argument is IrGetValue) {
                        val rootValue = argument.getRootValue()
                        if (expression.operator == IrTypeOperator.INSTANCEOF || expression.operator == IrTypeOperator.NOT_INSTANCEOF) {
                            val isSubtypeOfPredicate = Predicates.isSubtypeOf(rootValue, expression.typeOperand)
                            val fullIsSubtypeOfPredicate = andPredicates(predicate, isSubtypeOfPredicate)
                            val fullIsNotSubtypeOfPredicate = andPredicates(predicate, invertPredicate(isSubtypeOfPredicate))
                            return if (expression.operator == IrTypeOperator.INSTANCEOF)
                                BooleanPredicate(ifTrue = fullIsSubtypeOfPredicate, ifFalse = fullIsNotSubtypeOfPredicate)
                            else
                                BooleanPredicate(ifTrue = fullIsNotSubtypeOfPredicate, ifFalse = fullIsSubtypeOfPredicate)
                        }
                    }
                }

                val result = expression.accept(this, predicate)
                return BooleanPredicate(
                        ifTrue = andPredicates(Predicates.conjunctionOf(ComplexTerm(expression)), result),
                        ifFalse = andPredicates(Predicates.invertedConjunctionOf(ComplexTerm(expression)), result)
                )
            }

            private fun buildNullablePredicate(expression: IrExpression, predicate: Predicate): NullablePredicate? {
                if (expression is IrGetValue) {
                    val variableIsNullPredicate = Predicates.conjunctionOf(SimpleTerm.IsNull(expression.getRootValue()))
                    return NullablePredicate(
                            ifNull = andPredicates(predicate, variableIsNullPredicate),
                            ifNotNull = andPredicates(predicate, invertPredicate(variableIsNullPredicate))
                    )
                }
                if (expression is IrTypeOperatorCall && expression.operator == IrTypeOperator.SAFE_CAST) {
                    val argument = expression.argument
                    if (argument is IrGetValue) {
                        val isSubtypeOfPredicate = Predicates.isSubtypeOf(argument.getRootValue(), expression.typeOperand)
                        return NullablePredicate(
                                ifNull = andPredicates(predicate, invertPredicate(isSubtypeOfPredicate)),
                                ifNotNull = andPredicates(predicate, isSubtypeOfPredicate)
                        )
                    }
                }
                val matchResultSafeCall = expression.matchSafeCall()
                if (matchResultSafeCall != null) {
                    val (variable, type, result) = matchResultSafeCall
                    val variablePredicate = if (type == null)
                        Predicates.invertedConjunctionOf(SimpleTerm.IsNull(variable))
                    else
                        Predicates.isSubtypeOf(variable, type)
                    return NullablePredicate(
                            ifNull = andPredicates(predicate, invertPredicate(variablePredicate)),
                            ifNotNull = visitElement(result, andPredicates(predicate, variablePredicate))
                    )
                }

                return null
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

            fun IrValueDeclaration.getRootValue(): IrValueDeclaration {
                if (this is IrValueParameter || (this as IrVariable).isVar)
                    return this
                val initializerRootValue = (this.initializer as? IrGetValue)?.getRootValue()
                return if (initializerRootValue == null || (initializerRootValue as? IrVariable)?.isVar == true)
                    this
                else initializerRootValue
            }

            fun IrGetValue.getRootValue() = this.symbol.owner.getRootValue()

            override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Predicate): Predicate {
                /*
                  TYPE_OP type=<root>.A origin=IMPLICIT_CAST typeOperand=<root>.A
                    TYPE_OP type=<root>.A? origin=CAST typeOperand=<root>.A?
                      TYPE_OP type=kotlin.Any origin=IMPLICIT_CAST typeOperand=kotlin.Any
                        GET_VAR 'x: kotlin.Any declared in <root>.foo' type=kotlin.Any origin=null
                 */
                val result = expression.argument.accept(this, data)
                if (expression.isCast()) {
                    if (debugOutput) {
                        println("ZZZ: ${expression.dump()}")
                        println("    $result")
                        println()
                    }
                    val argument = expression.unwrapCasts()
                    if (argument is IrGetValue)
                        return andPredicates(result, Predicates.isSubtypeOf(argument.getRootValue(), expression.typeOperand))
                }
                return result
            }

//            override fun visitWhen(expression: IrWhen, data: Predicate): Predicate {
//                var predicate = data
//                var result: Predicate = Predicate.Zero
//                for (branch in expression.branches) {
//                    val conditionBooleanPredicate = buildBooleanPredicate(branch.condition, predicate)
//                    if (debugOutput) {
//                        println("QXX: ${branch.condition.dump()}")
//                        println("    ${conditionBooleanPredicate.ifTrue}")
//                        println("    ${conditionBooleanPredicate.ifFalse}")
//                        println("    $result")
//                        println()
//                    }
//                    result = orPredicates(result, branch.result.accept(this, conditionBooleanPredicate.ifTrue))
//                    predicate = conditionBooleanPredicate.ifFalse
//                }
//                if (debugOutput) {
//                    println("QXX")
//                    println("    $result")
//                    println("    $predicate")
//                    result = orPredicates(result, predicate)
//                    println("    $result")
//                    println()
//                }
//
//                return result
//            }

            override fun visitWhen(expression: IrWhen, data: Predicate): Predicate {
                var predicate = data
                for (branch in expression.branches) {
                    val conditionBooleanPredicate = buildBooleanPredicate(branch.condition, predicate)
                    if (debugOutput) {
                        println("QXX: ${branch.condition.dump()}")
                        println("    ${conditionBooleanPredicate.ifTrue}")
                        println("    ${conditionBooleanPredicate.ifFalse}")
                        println()
                    }
                    branch.result.accept(this, conditionBooleanPredicate.ifTrue)
                    predicate = conditionBooleanPredicate.ifFalse
                }
                if (debugOutput) {
                    println("QXX")
                    println("    $predicate")
                    println()
                }

                return data
            }

//            override fun visitVariable(declaration: IrVariable, data: Predicate): Predicate {
//                return super.visitVariable(declaration, data)
//            }
//
//            override fun visitSetValue(expression: IrSetValue, data: Predicate): Predicate {
//                return super.visitSetValue(expression, data)
//            }

        }, Predicate.One)
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

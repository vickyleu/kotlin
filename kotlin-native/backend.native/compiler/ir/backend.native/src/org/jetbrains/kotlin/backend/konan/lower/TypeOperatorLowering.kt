/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.isUnconditional
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.objcinterop.isObjCForwardDeclaration
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
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

internal val STATEMENT_ORIGIN_NO_CAST_NEEDED = IrStatementOriginImpl("NO_CAST_NEEDED")

internal class CastsOptimization(val context: Context, val computePreciseResultForWhens: Boolean) : BodyLoweringPass {
    private val not = context.irBuiltIns.booleanNotSymbol
    private val eqeq = context.irBuiltIns.eqeqSymbol
    private val eqeqeq = context.irBuiltIns.eqeqeqSymbol
    private val ieee754EqualsSymbols: Set<IrSimpleFunctionSymbol> =
            context.irBuiltIns.ieee754equalsFunByOperandType.values.toSet()
    private val throwClassCastException = context.ir.symbols.throwClassCastException

    private sealed class LeafTerm {
        abstract fun invert(): LeafTerm
    }

    private data class ComplexTerm(val element: IrElement, val value: Boolean) : LeafTerm() {
        override fun toString() = when (element) {
            is IrValueDeclaration -> if (value) "${element.name}" else "!${element.name}"
            else -> "[${element::class.java.simpleName}@0x${System.identityHashCode(element).toString(16)} is $value]"
        }

        override fun invert() = ComplexTerm(element, !value)
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

        fun isSubtypeOf(variable: IrValueDeclaration, type: IrType): Predicate {
            val variableIsNullable = variable.type.isNullable()
            val typeIsNullable = type.isNullable()
            val dstClass = type.erasedUpperBound
            val isSuperClassCast = variable.type.classifierOrNull !is IrTypeParameterSymbol // Due to unsafe casts, see unchecked_cast8.kt as an example.
                    && variable.type.isSubtypeOfClass(dstClass.symbol)
            return when {
                isSuperClassCast -> {
                    if (variableIsNullable && !typeIsNullable) // (variable: A?) is A = variable != null
                        disjunctionOf(SimpleTerm.IsNotNull(variable))
                    else Predicate.Empty
                }
                else -> {
                    if (variableIsNullable && typeIsNullable) // (variable: A?) is B? = variable == null || variable is B
                        disjunctionOf(SimpleTerm.IsNull(variable), SimpleTerm.Is(variable, dstClass))
                    else
                        disjunctionOf(SimpleTerm.Is(variable, dstClass))
                }
            }
        }

        fun optimizeAwayComplexTerms(predicate: Predicate): Predicate {
            val conjunction = predicate as? Conjunction ?: return predicate
            val terms = conjunction.terms.filterNot { disjunction -> disjunction.terms.any { it is ComplexTerm } }
            return if (terms.isEmpty()) Predicate.Empty else Conjunction(terms)
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

            val groupedComplexTerms = complexTerms.groupBy { it.element }
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

    private sealed class VariableValue {
        data object Ordinary : VariableValue()
        class BooleanPredicate(val predicate: CastsOptimization.BooleanPredicate) : VariableValue()
        class NullablePredicate(val predicate: CastsOptimization.NullablePredicate) : VariableValue()
    }

//    fun foo(x: Int, o: Any) {
//        var mutO = o
//        val y = x + x
//        if (mutO is String)
//            println((mutO as String).length)
//        else
//            println(y)
//        mutO = Any()
//        if (s != null)
//            println((mutO as? String)?.get(0)?.code ?: y)
//        else
//            println(x)
//    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        //if (container.fileOrNull?.path?.endsWith("z13.kt") != true) return
        //if (container.fileOrNull?.path?.endsWith("tt.kt") != true) return
        if (container.fileOrNull?.path?.endsWith("remove_redundant_type_checks.kt") != true) return
        //println("${container.fileOrNull?.path} ${container.render()}")
        val debugOutput = false
        //val debugOutput = container.fileOrNull?.path?.endsWith("collections/Arrays.kt") == true && (container as? IrFunction)?.name?.asString() == "contentDeepEqualsImpl"

        val typeCheckResults = mutableMapOf<IrTypeOperatorCall, Boolean>()
        irBody.accept(object : IrElementVisitor<Predicate, Predicate> {
            val upperLevelPredicates = mutableListOf<Predicate>()
            val variableValueCounters = mutableMapOf<IrVariable, Int>()
            val variableValues = mutableMapOf<IrValueDeclaration, VariableValue>()
            val variablePhiNodes = mutableMapOf<IrVariable, Set<IrValueDeclaration>>()

            fun createPhantomVariable(variable: IrVariable, value: IrExpression): IrVariable {
                val counter = variableValueCounters.getOrPut(variable) { 0 }
                variableValueCounters[variable] = counter + 1
                return IrVariableImpl(
                        value.startOffset, value.endOffset,
                        IrDeclarationOrigin.DEFINED,
                        IrVariableSymbolImpl(),
                        Name.identifier("${variable.name}\$$counter"),
                        value.type,
                        isVar = false,
                        isConst = false,
                        isLateinit = false,
                )
            }

            val IrVariable.isMutable: Boolean
                get() = isVar || initializer == null

            inline fun <R> usingUpperLevelPredicate(predicate: Predicate, block: () -> R): R {
                upperLevelPredicates.push(predicate)
                val result = block()
                upperLevelPredicates.pop()
                return result
            }

            fun getFullPredicate(currentPredicate: Predicate, optimizeAwayComplexTerms: Boolean) =
                    usingUpperLevelPredicate(currentPredicate) {
                        val initialPredicate: Predicate = Predicate.Empty
                        upperLevelPredicates.fold(initialPredicate) { acc, predicate ->
                            andPredicates(acc, if (optimizeAwayComplexTerms) Predicates.optimizeAwayComplexTerms(predicate) else predicate)
                        }
                    }

            fun buildForceSubtypeOfPredicate(variable: IrValueDeclaration, type: IrType): Predicate =
                    (variablePhiNodes[variable] ?: listOf(variable))
                            .fold(Predicate.Empty as Predicate) { acc, value ->
                                andPredicates(acc, Predicates.isSubtypeOf(value, type))
                            }

            fun buildIsNotSubtypeOfPredicate(variable: IrValueDeclaration, type: IrType): Predicate =
                    (variablePhiNodes[variable] ?: listOf(variable))
                            .fold(Predicate.False as Predicate) { acc, value ->
                                orPredicates(acc, invertPredicate(Predicates.isSubtypeOf(value, type)))
                            }

            fun buildIsSubtypeOfPredicate(variable: IrValueDeclaration, type: IrType): Predicate =
                    (variablePhiNodes[variable] ?: listOf(variable))
                            .fold(Predicate.False as Predicate) { acc, value ->
                                orPredicates(acc, Predicates.isSubtypeOf(value, type))
                            }

            fun buildIsNullPredicate(variable: IrValueDeclaration): Predicate =
                    (variablePhiNodes[variable] ?: listOf(variable))
                            .fold(Predicate.False as Predicate) { acc, value ->
                                orPredicates(acc, Predicates.disjunctionOf(SimpleTerm.IsNull(value)))
                            }

            fun buildIsNotNullPredicate(variable: IrValueDeclaration): Predicate =
                    (variablePhiNodes[variable] ?: listOf(variable))
                            .fold(Predicate.False as Predicate) { acc, value ->
                                orPredicates(acc, Predicates.disjunctionOf(SimpleTerm.IsNotNull(value)))
                            }

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

            fun IrExpression.matchSafeCall(): Triple<IrValueDeclaration, IrTypeOperatorCall?, IrExpression>? {
                val statements = (this as? IrBlock)?.statements?.takeIf { it.size == 2 } ?: return null
                val safeReceiver = statements[0] as? IrVariable ?: return null
                val initializer = safeReceiver.initializer
                val variableGetter: IrGetValue
                val typeOperatorCall: IrTypeOperatorCall?
                when (initializer) {
                    is IrGetValue -> {
                        variableGetter = initializer
                        typeOperatorCall = null
                    }
                    is IrTypeOperatorCall -> {
                        if (initializer.operator != IrTypeOperator.SAFE_CAST)
                            return null
                        variableGetter = initializer.argument.unwrapCasts() as? IrGetValue ?: return null
                        typeOperatorCall = initializer
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

                return Triple(variableGetter.symbol.owner, typeOperatorCall, safeCallResultWhen.branches[1].result)
            }

            fun orPredicates(leftPredicate: Predicate, rightPredicate: Predicate): Predicate = when {
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
                    // (a1 & a2 &.. ak) | (b1 & b2 &.. bl) = &[i=1..k, j=1..l] (ai | bj)
                    val resultDisjunctions = mutableListOf<Disjunction?>()
                    for (leftTerm in (leftPredicate as Conjunction).terms) {
                        for (rightTerm in (rightPredicate as Conjunction).terms) {
                            val currentPredicate = Predicates.disjunctionOf(leftTerm.terms + rightTerm.terms)
                            if (currentPredicate == Predicate.Empty)
                                continue
                            val currentDisjunction = (currentPredicate as Conjunction).terms.singleOrNull()!!
                            if (resultDisjunctions.any { it != null && currentDisjunction followsFrom it })
                                continue
                            for (i in resultDisjunctions.indices) {
                                val disjunction = resultDisjunctions[i]
                                if (disjunction != null && disjunction followsFrom currentDisjunction)
                                    resultDisjunctions[i] = null
                            }
                            resultDisjunctions.add(currentDisjunction)
                        }
                    }

                    val nonNullDisjunctions = resultDisjunctions.filterNotNull()
                    when {
                        nonNullDisjunctions.isEmpty() -> Predicate.Empty
                        else -> Conjunction(nonNullDisjunctions)
                    }.also {
                        if (debugOutput) {
                            println("OR: $leftPredicate")
                            println("    $rightPredicate")
                            println(" =  $it")
//                val t = IllegalStateException()
//                println(t.stackTraceToString())
                        }
                    }
                }
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
                    when (falseLeafIndices.cardinality()) {
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
                    when (falseLeafIndices.cardinality()) {
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

                return Conjunction(leftTerms + rightTerms)/*.also {
                    if (debugOutput) {
                        println("AND: $leftPredicate")
                        println("    $rightPredicate")
                        println(" =  $it")
                        //                val t = IllegalStateException()
                        //                println(t.stackTraceToString())
                    }
                }*/
            }

            fun invertPredicate(predicate: Predicate): Predicate = when (predicate) {
                //Predicate.True -> Predicate.False
                Predicate.False -> Predicate.Empty
                Predicate.Empty -> Predicate.False
//                is Conjunction -> {
//                    // ~(() & () & .. ()) = ~() | ~() | .. ~()
//                    val resultDisjunctions = mutableListOf<Disjunction?>()
//
//                    fun iterateOverTerms(k: Int, terms: Array<LeafTerm?>) {
//                        outerLoop@ for (term in predicate.terms[k].terms) {
//                            val invertedTerm = term.invert()
//                            for (i in 0..<k) {
//                                if (terms[i] == invertedTerm)
//                                    continue@outerLoop
//                            }
//                            terms[k] = invertedTerm
//                            val currentPredicate = Predicates.disjunctionOf(terms.take(k + 1).map { it!! })
//                            if (currentPredicate == Predicate.Empty)
//                                continue
//                            val currentDisjunction = (currentPredicate as Conjunction).terms.singleOrNull()!!
//                            if (resultDisjunctions.any { it != null && currentDisjunction followsFrom it })
//                                continue
//                            if (k < predicate.terms.size - 1)
//                                iterateOverTerms(k + 1, terms)
//                            else {
//                                for (i in resultDisjunctions.indices) {
//                                    val disjunction = resultDisjunctions[i]
//                                    if (disjunction != null && disjunction followsFrom currentDisjunction)
//                                        resultDisjunctions[i] = null
//                                }
//                                resultDisjunctions.add(currentDisjunction)
//                            }
//                        }
//                    }
//
////                    println("INVERT: $predicate")
//
//                    iterateOverTerms(0, arrayOfNulls(predicate.terms.size))
//
//                    val nonNullDisjunctions = resultDisjunctions.filterNotNull()
//                    if (nonNullDisjunctions.isEmpty())
//                        Predicate.Empty
//                    else Conjunction(nonNullDisjunctions)
//                }
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

            fun buildBooleanPredicate(expression: IrExpression): BooleanPredicate {
                return buildBooleanPredicateImpl(expression, Predicate.Empty)
//                val result = buildBooleanPredicateImpl(expression, Predicate.Empty)
//                return BooleanPredicate(
//                        ifTrue = Predicates.optimizeAwayComplexTerms(result.ifTrue),
//                        ifFalse = Predicates.optimizeAwayComplexTerms(result.ifFalse),
//                )
            }

            /*
            val o = if (f) a is T else b is T
            if (o) => a is T | b is T
            if (!o) => a !is T | b !is T
            }
             */
            fun buildBooleanPredicateImpl(variable: IrValueDeclaration): BooleanPredicate =
                    variablePhiNodes[variable]?.let { phiNode ->
                        val predicates = phiNode.map { buildBooleanPredicateImpl(it) }
                        BooleanPredicate(
                                ifTrue = predicates.fold(Predicate.False as Predicate) { acc, booleanPredicate ->
                                    orPredicates(acc, booleanPredicate.ifTrue)
                                },
                                ifFalse = predicates.fold(Predicate.False as Predicate) { acc, booleanPredicate ->
                                    orPredicates(acc, booleanPredicate.ifFalse)
                                }
                        )
                    } ?: when (val variableValue = variableValues[variable]) {
                        null, VariableValue.Ordinary -> {
                            BooleanPredicate(
                                    ifTrue = Predicates.disjunctionOf(ComplexTerm(variable, true)),
                                    ifFalse = Predicates.disjunctionOf(ComplexTerm(variable, false))
                            )
                        }
                        is VariableValue.BooleanPredicate -> variableValue.predicate
                        is VariableValue.NullablePredicate -> error("Unexpected nullable predicate for ${variable.render()}")
                    }

            fun buildBooleanPredicateImpl(expression: IrExpression, predicate: Predicate): BooleanPredicate {
                if (expression is IrGetValue) {
                    val variableBooleanPredicate = buildBooleanPredicateImpl(expression.symbol.owner)
                    return BooleanPredicate(
                            ifTrue = andPredicates(predicate, variableBooleanPredicate.ifTrue),
                            ifFalse = andPredicates(predicate, variableBooleanPredicate.ifFalse)
                    )
                }

                val matchResultAndAnd = expression.matchAndAnd()
                if (matchResultAndAnd != null) {
                    val (left, right) = matchResultAndAnd
                    val leftBooleanPredicate = buildBooleanPredicateImpl(left, predicate)
                    val rightBooleanPredicate = buildBooleanPredicateImpl(right, leftBooleanPredicate.ifTrue)
                    return BooleanPredicate(
                            ifTrue = rightBooleanPredicate.ifTrue,
                            ifFalse = orPredicates(leftBooleanPredicate.ifFalse, rightBooleanPredicate.ifFalse)
                    )
                }
                val matchResultOrOr = expression.matchOrOr()
                if (matchResultOrOr != null) {
                    val (left, right) = matchResultOrOr
                    val leftBooleanPredicate = buildBooleanPredicateImpl(left, predicate)
                    val rightBooleanPredicate = buildBooleanPredicateImpl(right, leftBooleanPredicate.ifFalse)
                    return BooleanPredicate(
                            ifTrue = orPredicates(leftBooleanPredicate.ifTrue, rightBooleanPredicate.ifTrue),
                            ifFalse = rightBooleanPredicate.ifFalse
                    )
                }
                val matchResultNot = expression.matchNot()
                if (matchResultNot != null) {
                    return buildBooleanPredicateImpl(matchResultNot, predicate).invert()
                }

                // if (x as? A != null) ...  =  if (x is A) ...
                // if ((x as? A)?.y == ..)
                val matchResultEquality = expression.matchEquality()
                if (matchResultEquality != null) {
                    val (left, right) = matchResultEquality
                    val leftIsNullConst = left.isNullConst()
                    val rightIsNullConst = right.isNullConst()
                    return if ((leftIsNullConst || !left.type.isNullable()) && right.type.isNullable()) {
                        val leftPredicate = if (leftIsNullConst)
                            Predicate.Empty
                        else
                            left.accept(this, predicate)
                        val nullablePredicate = buildNullablePredicateImpl(right, leftPredicate)
                        if (nullablePredicate == null) {
                            val result = right.accept(this, leftPredicate)
                            BooleanPredicate(
                                    ifTrue = andPredicates(Predicates.disjunctionOf(ComplexTerm(expression, value = true)), result),
                                    ifFalse = andPredicates(Predicates.disjunctionOf(ComplexTerm(expression, value = false)), result)
                            )
                        } else if (leftIsNullConst) {
                            BooleanPredicate(
                                    ifTrue = nullablePredicate.ifNull,
                                    ifFalse = nullablePredicate.ifNotNull
                            )
                        } else {
                            BooleanPredicate(
                                    ifTrue = andPredicates(
                                            nullablePredicate.ifNotNull,
                                            Predicates.disjunctionOf(ComplexTerm(expression, value = true))
                                    ),
                                    ifFalse = orPredicates(
                                            nullablePredicate.ifNull,
                                            andPredicates(nullablePredicate.ifNotNull, Predicates.disjunctionOf(ComplexTerm(expression, value = false)))
                                    )
                            )
                        }
                    } else if ((rightIsNullConst || !right.type.isNullable()) && left.type.isNullable()) {
                        val nullablePredicate = buildNullablePredicateImpl(left, predicate)
                        return if (nullablePredicate == null) {
                            val result = expression.accept(this, predicate)
                            BooleanPredicate(
                                    ifTrue = andPredicates(Predicates.disjunctionOf(ComplexTerm(expression, value = true)), result),
                                    ifFalse = andPredicates(Predicates.disjunctionOf(ComplexTerm(expression, value = false)), result)
                            )
                        } else if (rightIsNullConst) {
                            BooleanPredicate(
                                    ifTrue = nullablePredicate.ifNull,
                                    ifFalse = nullablePredicate.ifNotNull
                            )
                        } else {
                            val localRightPredicate = usingUpperLevelPredicate(orPredicates(nullablePredicate.ifNull, nullablePredicate.ifNotNull)) {
                                right.accept(this, Predicate.Empty)
                            }
                            BooleanPredicate(
                                    ifTrue = andPredicates(
                                            andPredicates(
                                                    nullablePredicate.ifNotNull,
                                                    Predicates.disjunctionOf(ComplexTerm(expression, value = true))
                                            ),
                                            localRightPredicate
                                    ),
                                    ifFalse = andPredicates(
                                            orPredicates(
                                                    nullablePredicate.ifNull,
                                                    andPredicates(
                                                            nullablePredicate.ifNotNull,
                                                            Predicates.disjunctionOf(ComplexTerm(expression, value = false))
                                                    )
                                            ),
                                            localRightPredicate
                                    )
                            )
                        }
                    } else {
                        val result = expression.accept(this, predicate)
                        return BooleanPredicate(
                                ifTrue = andPredicates(Predicates.disjunctionOf(ComplexTerm(expression, value = true)), result),
                                ifFalse = andPredicates(Predicates.disjunctionOf(ComplexTerm(expression, value = false)), result)
                        )
                    }
                }

                if ((expression as? IrConst)?.value == true) {
                    return BooleanPredicate(ifTrue = predicate, ifFalse = Predicate.False)
                }
                if ((expression as? IrConst)?.value == false) {
                    return BooleanPredicate(ifTrue = Predicate.False, ifFalse = predicate)
                }
                if (expression is IrTypeOperatorCall && expression.isTypeCheck()) {
                    val argument = expression.argument.unwrapCasts()
                    if (argument is IrGetValue) {
                        val variable = argument.symbol.owner
                        val argumentPredicate = expression.argument.accept(this, predicate)
                        tryOptimizeTypeCheck(expression, variable, argumentPredicate)
                        val fullIsSubtypeOfPredicate = andPredicates(argumentPredicate, buildIsSubtypeOfPredicate(variable, expression.typeOperand))
                        val fullIsNotSubtypeOfPredicate = andPredicates(argumentPredicate, buildIsNotSubtypeOfPredicate(variable, expression.typeOperand))
                        return if (expression.operator == IrTypeOperator.INSTANCEOF)
                            BooleanPredicate(ifTrue = fullIsSubtypeOfPredicate, ifFalse = fullIsNotSubtypeOfPredicate)
                        else
                            BooleanPredicate(ifTrue = fullIsNotSubtypeOfPredicate, ifFalse = fullIsSubtypeOfPredicate)
                    }
                }

                val result = expression.accept(this, predicate)
                return BooleanPredicate(
                        ifTrue = andPredicates(Predicates.disjunctionOf(ComplexTerm(expression, value = true)), result),
                        ifFalse = andPredicates(Predicates.disjunctionOf(ComplexTerm(expression, value = false)), result)
                )
            }

            fun buildNullablePredicate(expression: IrExpression): NullablePredicate? {
                return buildNullablePredicateImpl(expression, Predicate.Empty)
            }

            /*
            val o = if (f) a as? T else b as? T
            if (o == null) => a !is T | b !is T
            if (o != null) => a is T | b is T
             */
            fun buildNullablePredicateImpl(variable: IrValueDeclaration): NullablePredicate =
                    variablePhiNodes[variable]?.let { phiNode ->
                        val predicates = phiNode.map { buildNullablePredicateImpl(it) }
                        NullablePredicate(
                                ifNull = predicates.fold(Predicate.False as Predicate) { acc, nullablePredicate ->
                                    orPredicates(acc, nullablePredicate.ifNull)
                                },
                                ifNotNull = predicates.fold(Predicate.False as Predicate) { acc, nullablePredicate ->
                                    orPredicates(acc, nullablePredicate.ifNotNull)
                                }
                        )
                    } ?: when (val variableValue = variableValues[variable]) {
                        null, VariableValue.Ordinary -> {
                            NullablePredicate(
                                    ifNull = Predicates.disjunctionOf(SimpleTerm.IsNull(variable)),
                                    ifNotNull = Predicates.disjunctionOf(SimpleTerm.IsNotNull(variable))
                            )
                        }
                        is VariableValue.NullablePredicate -> variableValue.predicate
                        is VariableValue.BooleanPredicate -> error("Unexpected boolean predicate for ${variable.render()}")
                    }

            private fun buildNullablePredicateImpl(expression: IrExpression, predicate: Predicate): NullablePredicate? {
                if (!expression.type.isNullable())
                    return NullablePredicate(ifNull = Predicate.False, ifNotNull = predicate)
                if (expression is IrGetValue) {
                    val variableNullablePredicate = buildNullablePredicateImpl(expression.symbol.owner)
                    return NullablePredicate(
                            ifNull = andPredicates(predicate, variableNullablePredicate.ifNull),
                            ifNotNull = andPredicates(predicate, variableNullablePredicate.ifNotNull)
                    )
                }
                if (expression is IrTypeOperatorCall && expression.operator == IrTypeOperator.SAFE_CAST) {
                    val argument = expression.argument.unwrapCasts()
                    if (argument is IrGetValue) {
                        val variable = argument.symbol.owner
                        val argumentPredicate = expression.argument.accept(this, predicate)
                        tryOptimizeTypeCheck(expression, variable, argumentPredicate)
                        return NullablePredicate(
                                ifNull = andPredicates(argumentPredicate, buildIsNotSubtypeOfPredicate(variable, expression.typeOperand)),
                                ifNotNull = andPredicates(argumentPredicate, buildIsSubtypeOfPredicate(variable, expression.typeOperand))
                        )
                    }
                }
                val matchResultSafeCall = expression.matchSafeCall()
                if (matchResultSafeCall != null) {
                    val (variable, typeOperatorCall, result) = matchResultSafeCall
                    val variablePredicate: NullablePredicate
                    val argumentPredicate: Predicate
                    if (typeOperatorCall == null) {
                        argumentPredicate = predicate
                        variablePredicate = NullablePredicate(
                                ifNull = buildIsNullPredicate(variable),
                                ifNotNull = buildIsNotNullPredicate(variable)
                        )
                    } else {
                        argumentPredicate = typeOperatorCall.argument.accept(this, predicate)
                        tryOptimizeTypeCheck(typeOperatorCall, variable, argumentPredicate)
                        variablePredicate = NullablePredicate(
                                ifNull = buildIsNotSubtypeOfPredicate(variable, typeOperatorCall.typeOperand),
                                ifNotNull = buildIsSubtypeOfPredicate(variable, typeOperatorCall.typeOperand)
                        )
                    }
                    return NullablePredicate(
                            ifNull = andPredicates(argumentPredicate, variablePredicate.ifNull),
                            ifNotNull = result.accept(this, andPredicates(argumentPredicate, variablePredicate.ifNotNull))
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

            override fun visitBlock(expression: IrBlock, data: Predicate): Predicate {
                val result = super.visitBlock(expression, data)
                return if (expression is IrReturnableBlock) data else result
            }

            override fun visitReturn(expression: IrReturn, data: Predicate): Predicate {
                expression.value.accept(this, data)
                return Predicate.False
            }

            fun IrTypeOperatorCall.isCast() =
                    operator == IrTypeOperator.CAST || operator == IrTypeOperator.IMPLICIT_CAST

            fun IrTypeOperatorCall.isTypeCheck() =
                    operator == IrTypeOperator.INSTANCEOF || operator == IrTypeOperator.NOT_INSTANCEOF

            fun IrExpression.unwrapCasts(): IrExpression {
                var result = this
                while ((result as? IrTypeOperatorCall)?.isCast() == true)
                    result = result.argument
                return result
            }

            fun tryOptimizeTypeCheck(expression: IrTypeOperatorCall, variable: IrValueDeclaration, predicate: Predicate) {
                val fullPredicate = getFullPredicate(predicate, true)
                if (debugOutput) {
                    println("ZZZ: ${expression.dump()}")
                    println("    $fullPredicate")
                }
                // Check if (predicate & (v !is T)) is identically equal to false: meaning the cast will always succeed.
                // Similarly, if (predicate & (v is T)) is identically equal to false, then the cast will never succeed.
                // Note: further improvement will be to check not only for identical equality to false but actually try to
                // find the combination of leaf terms satisfying the predicate (though it can be computationally unfeasible).
                val castIsFailedPredicate = andPredicates(fullPredicate, buildIsNotSubtypeOfPredicate(variable, expression.typeOperand))
                if (debugOutput) {
                    println("    castIsFailedPredicate: $castIsFailedPredicate")
                }
                if (castIsFailedPredicate == Predicate.False) {
                    // The cast will always succeed.
                    typeCheckResults[expression] = true
                }
                val castIsSuccessfulPredicate = andPredicates(fullPredicate, buildIsSubtypeOfPredicate(variable, expression.typeOperand))
                if (debugOutput) {
                    println("    castIsSuccessfulPredicate: $castIsSuccessfulPredicate")
                    println()
                }
                if (castIsSuccessfulPredicate == Predicate.False) {
                    // The cast will never succeed.
                    typeCheckResults[expression] = false
                }
            }

            // TODO: Think about other possible arguments which might be optimized (IrWhen, IrReturnableBlock, ...)
            override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Predicate): Predicate {
                /*
                  TYPE_OP type=<root>.A origin=IMPLICIT_CAST typeOperand=<root>.A
                    TYPE_OP type=<root>.A? origin=CAST typeOperand=<root>.A?
                      TYPE_OP type=kotlin.Any origin=IMPLICIT_CAST typeOperand=kotlin.Any
                        GET_VAR 'x: kotlin.Any declared in <root>.foo' type=kotlin.Any origin=null
                 */
                val argumentPredicate = expression.argument.accept(this, data)
                if (expression.isCast() || expression.isTypeCheck() || expression.operator == IrTypeOperator.SAFE_CAST) {
//                    val fullPredicate = getFullPredicate(result, true)
//                    if (debugOutput) {
//                        println("ZZZ: ${expression.dump()}")
//                        println("    $fullPredicate")
//                    }
                    val argument = expression.argument.unwrapCasts()
                    if (argument is IrGetValue) {
                        val variable = argument.symbol.owner
                        tryOptimizeTypeCheck(expression, variable, argumentPredicate)
//                        val isSubtypeOfPredicate = Predicates.isSubtypeOf(argument.getRootValue(), expression.typeOperand)
//                        // Check if (result & (v !is T)) is identically equal to false: meaning the cast will always succeed.
//                        // Similarly, if (result & (v is T)) is identically equal to false, then the cast will never succeed.
//                        // Note: further improvement will be to check not only for identical equality to false but actually try to
//                        // find the combination of leaf terms satisfying the predicate (though it can be computationally unfeasible).
//                        val castIsFailedPredicate = andPredicates(fullPredicate, invertPredicate(isSubtypeOfPredicate))
//                        if (debugOutput) {
//                            println("    castIsFailedPredicate: $castIsFailedPredicate")
//                        }
//                        if (castIsFailedPredicate == Predicate.False) {
//                            // The cast will always succeed.
//                            typeCheckResults[expression] = true
//                        }
//                        val castIsSuccessfulPredicate = andPredicates(fullPredicate, isSubtypeOfPredicate)
//                        if (debugOutput) {
//                            println("    castIsSuccessfulPredicate: $castIsSuccessfulPredicate")
//                            println()
//                        }
//                        if (castIsSuccessfulPredicate == Predicate.False) {
//                            // The cast will never succeed.
//                            typeCheckResults[expression] = false
//                        }

                        return if (expression.isCast())
                            andPredicates(argumentPredicate, buildForceSubtypeOfPredicate(variable, expression.typeOperand))
                        else argumentPredicate
                    }
//                    if (debugOutput) {
//                        println()
//                    }
                }
                return argumentPredicate
            }

            override fun visitWhen(expression: IrWhen, data: Predicate): Predicate {
                upperLevelPredicates.push(data)
                var predicate: Predicate = Predicate.Empty
                var result: Predicate = Predicate.Empty
                var isFirstBranch = true
                for (branch in expression.branches) {
                    upperLevelPredicates.push(predicate)
                    val conditionBooleanPredicate = buildBooleanPredicate(branch.condition)
                    if (debugOutput) {
                        println("QXX: ${branch.condition.dump()}")
                        println("    upperLevelPredicate = ${getFullPredicate(Predicate.Empty, false)}")
                        println("    condition = ${conditionBooleanPredicate.ifTrue}")
                        println("    ~condition = ${conditionBooleanPredicate.ifFalse}")
                        println("    result = $result")
                        println()
                    }
                    val branchResultPredicate = andPredicates(predicate, branch.result.accept(this, conditionBooleanPredicate.ifTrue))
                    if (computePreciseResultForWhens) {
                        result = orPredicates(result, branchResultPredicate)
                    } else {
                        if (isFirstBranch)
                            result = orPredicates(conditionBooleanPredicate.ifTrue, conditionBooleanPredicate.ifFalse)
                    }
                    isFirstBranch = false
                    predicate = andPredicates(predicate, conditionBooleanPredicate.ifFalse)
                    upperLevelPredicates.pop()
                }
                if (debugOutput) {
                    println("QXX")
                    println("    result = $result")
                    println("    predicate = $predicate")
                }
                if (computePreciseResultForWhens)
                    result = orPredicates(result, predicate)
                if (debugOutput) {
                    println("    result = $result")
                }
                result = Predicates.optimizeAwayComplexTerms(result)
                if (debugOutput)
                    println("    result = $result")
                upperLevelPredicates.pop()
                result = andPredicates(data, result)
                if (debugOutput) {
                    println("    result = $result")
                    println()
                }
                return result
            }

            // TODO: If value is IrWhen with a bunch of IrGetValue, should we get a phi node here?
            fun setVariable(variable: IrVariable, value: IrExpression, data: Predicate): Predicate {
                if (value is IrGetValue) {
                    val delegatedVariable = value.symbol.owner
                    variablePhiNodes[variable] = variablePhiNodes[delegatedVariable] ?: setOf(delegatedVariable)
                    return data
                }

                val actualVariable = if (variable.isMutable)
                    createPhantomVariable(variable, value).also { variablePhiNodes[variable] = setOf(it) }
                else variable

                if (variable.type.isNullable()) {
                    val predicate = usingUpperLevelPredicate(data) { buildNullablePredicate(value) }
                    if (predicate != null) {
                        variableValues[actualVariable] = VariableValue.NullablePredicate(predicate)
                        return data
                    }
                } else if (variable.type.isBoolean()) {
                    val predicate = usingUpperLevelPredicate(data) { buildBooleanPredicate(value) }
                    variableValues[actualVariable] = VariableValue.BooleanPredicate(predicate)
                    return data
                }

                return value.accept(this, data)
            }

            override fun visitVariable(declaration: IrVariable, data: Predicate): Predicate {
                val initializer = declaration.initializer ?: return data
                return setVariable(declaration, initializer, data)
            }

            override fun visitSetValue(expression: IrSetValue, data: Predicate): Predicate {
                val variable = expression.symbol.owner as? IrVariable ?: error("Unexpected set to ${expression.symbol.owner.render()}")
                return setVariable(variable, expression.value, data)
            }

        }, Predicate.Empty)

        if (typeCheckResults.isEmpty()) return
        val irBuilder = context.createIrBuilder(container.symbol)
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
                expression.transformChildrenVoid()

                val typeCheckResult = typeCheckResults[expression] ?: return expression
                return when (expression.operator) {
                    IrTypeOperator.INSTANCEOF -> irBuilder.at(expression).irBoolean(typeCheckResult)
                    IrTypeOperator.NOT_INSTANCEOF -> irBuilder.at(expression).irBoolean(!typeCheckResult)
                    IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST, IrTypeOperator.SAFE_CAST -> when {
                        typeCheckResult -> irBuilder.at(expression).irBlock(origin = STATEMENT_ORIGIN_NO_CAST_NEEDED) {
                            +irImplicitCast(expression.argument, expression.typeOperand)
                        }
                        else -> if (expression.operator == IrTypeOperator.SAFE_CAST)
                            irBuilder.at(expression).irNull()
                        else
                            irBuilder.at(expression).irCall(throwClassCastException).apply {
                                val typeOperandClass = expression.typeOperand.erasedUpperBound
                                val typeOperandClassReference = IrClassReferenceImpl(
                                        startOffset, endOffset,
                                        context.ir.symbols.nativePtrType,
                                        typeOperandClass.symbol,
                                        typeOperandClass.defaultType
                                )
                                putValueArgument(0, expression.argument)
                                putValueArgument(1, typeOperandClassReference)
                            }
                    }
                    else -> error("Unexpected type operator: ${expression.operator}")
                }
            }
        })
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
                        thenPart = irBlock(origin = STATEMENT_ORIGIN_NO_CAST_NEEDED) {
                            +irImplicitCast(irGet(variable.owner), typeOperand)
                        },
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

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

internal class CastsOptimization(val context: Context) : BodyLoweringPass {
    private val not = context.irBuiltIns.booleanNotSymbol
    private val eqeq = context.irBuiltIns.eqeqSymbol
    private val eqeqeq = context.irBuiltIns.eqeqeqSymbol
    private val ieee754EqualsSymbols: Set<IrSimpleFunctionSymbol> =
            context.irBuiltIns.ieee754equalsFunByOperandType.values.toSet()
    private val throwClassCastException = context.ir.symbols.throwClassCastException
    private val unitType = context.irBuiltIns.unitType

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

        fun or(leftPredicate: Predicate, rightPredicate: Predicate): Predicate = when {
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
                        val currentPredicate = disjunctionOf(leftTerm.terms + rightTerm.terms)
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
                }/*.also {
                    if (debugOutput) {
                        println("OR: $leftPredicate")
                        println("    $rightPredicate")
                        println(" =  $it")
//                            val t = IllegalStateException()
//                            println(t.stackTraceToString())
                    }
                }*/
            }
        }

        fun and(leftPredicate: Predicate, rightPredicate: Predicate): Predicate {
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

        fun invert(predicate: Predicate): Predicate = when (predicate) {
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
                    or(
                            invert(Conjunction(listOf(predicate.terms.first()))),
                            invert(Conjunction(predicate.terms.drop(1)))
                    )
                }
            }
        }
    }

    private data class BooleanPredicate(val ifTrue: Predicate, val ifFalse: Predicate) {
        fun invert() = BooleanPredicate(ifFalse, ifTrue)
    }

    private data class NullablePredicate(val ifNull: Predicate, val ifNotNull: Predicate) {
        fun invert() = NullablePredicate(ifNotNull, ifNull)
    }

    private class PredicateHolder {
        var predicate: Predicate = Predicate.Empty
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
        class BooleanPredicate(val predicate: CastsOptimization.BooleanPredicate) : VariableValue()
        class NullablePredicate(val predicate: CastsOptimization.NullablePredicate) : VariableValue()
    }

    private class ControlFlowMergePointInfo(
            val level: Int,
            var phiNodeAlias: IrValueDeclaration? = null,
            var predicate: Predicate = Predicate.Empty,
            val variableAliases: MutableMap<IrVariable, IrValueDeclaration> = mutableMapOf()
    ) {
        constructor(other: ControlFlowMergePointInfo)
                : this(other.level, other.phiNodeAlias, other.predicate, other.variableAliases.toMutableMap())

        fun contentEquals(other: ControlFlowMergePointInfo): Boolean {
            if (level != other.level) return false
            if (phiNodeAlias != other.phiNodeAlias) return false
            if (variableAliases.size != other.variableAliases.size) return false
            for ((variable, alias) in variableAliases)
                if (other.variableAliases[variable] != alias) return false
//            val conjunction = predicate as? Conjunction
//            val otherConjunction = other.predicate as? Conjunction
//            if (conjunction == null || otherConjunction == null)
//                return predicate === other.predicate
//            if (conjunction.terms.size != otherConjunction.terms.size)
//                return false
//            for (term in conjunction.terms)
//                if (!otherConjunction.terms.any { term followsFrom it })
//                    return false
//            for (term in otherConjunction.terms)
//                if (!conjunction.terms.any { term followsFrom it })
//                    return false
//            return true

            return Predicates.and(
                    Predicates.or(Predicates.invert(predicate), other.predicate),
                    Predicates.or(predicate, Predicates.invert(other.predicate)),
            ) == Predicate.Empty
        }
    }

    private data class VisitorResult(val predicate: Predicate, val resultVariable: IrValueDeclaration?) {
        companion object {
            val Nothing = VisitorResult(Predicate.False, null)
        }
    }

    private enum class TypeCheckResult {
        ALWAYS_SUCCEEDS,
        NEVER_SUCCEEDS,
        UNKNOWN
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        //if (container.fileOrNull?.path?.endsWith("z15.kt") != true) return
        //if (container.fileOrNull?.path?.endsWith("tt.kt") != true) return
        if (container.fileOrNull?.path?.endsWith("remove_redundant_type_checks.kt") != true) return
        //if (container.fileOrNull?.path?.endsWith("test/Assertions.kt") != true) return
        //println(container.dump())
        //println("${container.fileOrNull?.path} ${container.render()}")
        val debugOutput = false
        //val debugOutput = container.fileOrNull?.path?.endsWith("collections/Arrays.kt") == true && (container as? IrFunction)?.name?.asString() == "contentDeepEqualsImpl"

        val typeCheckResults = mutableMapOf<IrTypeOperatorCall, TypeCheckResult>()
        irBody.accept(object : IrElementVisitor<VisitorResult, Predicate> {
            val upperLevelPredicates = mutableListOf<Predicate>()
            val variableValueCounters = mutableMapOf<IrVariable, Int>()
            val phantomVariables = mutableMapOf<IrExpression, IrVariable>()
            val variableValues = mutableMapOf<IrValueDeclaration, VariableValue>()

            val multipleValuesMarker = createVariable(irBody.startOffset, irBody.endOffset, "\$TheMarker", unitType)
            val variableAliases = mutableMapOf<IrVariable, IrValueDeclaration>()
            val returnableBlockCFMPInfos = mutableMapOf<IrReturnableBlock, ControlFlowMergePointInfo>()
            val breaksCFMPInfos = mutableMapOf<IrLoop, ControlFlowMergePointInfo>()
            val continuesCFMPInfos = mutableMapOf<IrLoop, ControlFlowMergePointInfo>()

            fun createVariable(startOffset: Int, endOffset: Int, name: String, type: IrType) =
                    IrVariableImpl(
                            startOffset, endOffset,
                            IrDeclarationOrigin.DEFINED,
                            IrVariableSymbolImpl(),
                            Name.identifier(name),
                            type,
                            isVar = false,
                            isConst = false,
                            isLateinit = false,
                    )

            fun createPhantomVariable(variable: IrVariable, startOffset: Int, endOffset: Int, type: IrType): IrVariable {
                val counter = variableValueCounters.getOrPut(variable) { 0 }
                variableValueCounters[variable] = counter + 1
                return createVariable(startOffset, endOffset, "${variable.name}\$$counter", type)
            }

            fun createPhantomVariable(variable: IrVariable, value: IrExpression) = phantomVariables.getOrPut(value) {
                createPhantomVariable(variable, value.startOffset, value.endOffset, value.type)
            }

            fun controlFlowMergePoint(cfmpInfo: ControlFlowMergePointInfo, result: VisitorResult) {
                for ((variable, alias) in variableAliases) {
                    val accumulatedAlias = cfmpInfo.variableAliases[variable]
                    if (accumulatedAlias == null)
                        cfmpInfo.variableAliases[variable] = alias
                    else if (accumulatedAlias != alias && accumulatedAlias != multipleValuesMarker)
                        cfmpInfo.variableAliases[variable] = multipleValuesMarker
                }
                val resultVariable = result.resultVariable
                if (resultVariable != null) {
                    if (cfmpInfo.phiNodeAlias == null)
                        cfmpInfo.phiNodeAlias = resultVariable
                    else if (cfmpInfo.phiNodeAlias != resultVariable)
                        cfmpInfo.phiNodeAlias = multipleValuesMarker
                }
                cfmpInfo.predicate = Predicates.or(
                        cfmpInfo.predicate,
                        getFullPredicate(result.predicate, false, cfmpInfo.level)
                )
            }

            fun finishControlFlowMerging(irElement: IrElement, cfmpInfo: ControlFlowMergePointInfo): VisitorResult {
                variableAliases.clear()
                for ((variable, alias) in cfmpInfo.variableAliases) {
                    variableAliases[variable] = if (alias != multipleValuesMarker)
                        alias
                    else
                        createPhantomVariable(variable, irElement.startOffset, irElement.endOffset, variable.type)
                }
                return VisitorResult(
                        Predicates.optimizeAwayComplexTerms(cfmpInfo.predicate),
                        cfmpInfo.phiNodeAlias.takeIf { it != multipleValuesMarker }
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

            fun getFullPredicate(currentPredicate: Predicate, optimizeAwayComplexTerms: Boolean, level: Int) =
                    usingUpperLevelPredicate(currentPredicate) {
                        val initialPredicate: Predicate = Predicate.Empty
                        upperLevelPredicates.drop(level).fold(initialPredicate) { acc, predicate ->
                            Predicates.and(acc, if (optimizeAwayComplexTerms) Predicates.optimizeAwayComplexTerms(predicate) else predicate)
                        }
                    }

            fun buildIsNotSubtypeOfPredicate(variable: IrValueDeclaration, type: IrType): Predicate =
                    Predicates.invert(Predicates.isSubtypeOf((variableAliases[variable] ?: variable), type))

            fun buildIsSubtypeOfPredicate(variable: IrValueDeclaration, type: IrType): Predicate =
                    Predicates.isSubtypeOf((variableAliases[variable] ?: variable), type)

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

            fun IrExpression.matchSafeCall(): Pair<IrExpression, IrExpression>? {
                val statements = (this as? IrBlock)?.statements?.takeIf { it.size == 2 } ?: return null
                val safeReceiver = statements[0] as? IrVariable ?: return null
                val initializer = safeReceiver.initializer ?: return null
//                val variableGetter: IrGetValue
//                val typeOperatorCall: IrTypeOperatorCall?
//                when (initializer) {
//                    is IrGetValue -> {
//                        variableGetter = initializer
//                        typeOperatorCall = null
//                    }
//                    is IrTypeOperatorCall -> {
//                        if (initializer.operator != IrTypeOperator.SAFE_CAST)
//                            return null
//                        variableGetter = initializer.argument.unwrapCasts() as? IrGetValue ?: return null
//                        typeOperatorCall = initializer
//                    }
//                    else -> return null
//                }
                val safeCallResultWhen = (statements[1] as? IrWhen)?.takeIf { it.branches.size == 2 } ?: return null
                val equalityMatchResult = safeCallResultWhen.branches[0].condition.matchEquality() ?: return null
                if ((equalityMatchResult.first as? IrGetValue)?.symbol?.owner != safeReceiver
                        || !equalityMatchResult.second.isNullConst()
                        || !safeCallResultWhen.branches[0].result.isNullConst())
                    return null
                if (!safeCallResultWhen.branches[1].isUnconditional())
                    return null

                //return Triple(variableGetter.symbol.owner, typeOperatorCall, safeCallResultWhen.branches[1].result)
                return Pair(initializer, safeCallResultWhen.branches[1].result)
            }

            fun buildBooleanPredicate(variable: IrValueDeclaration): BooleanPredicate =
                    variableAliases[variable]?.let { buildBooleanPredicate(it) }
                            ?: when (val variableValue = variableValues[variable]) {
                                null -> {
                                    BooleanPredicate(
                                            ifTrue = Predicates.disjunctionOf(ComplexTerm(variable, true)),
                                            ifFalse = Predicates.disjunctionOf(ComplexTerm(variable, false))
                                    )
                                }
                                is VariableValue.BooleanPredicate -> variableValue.predicate
                                is VariableValue.NullablePredicate -> error("Unexpected nullable predicate for ${variable.render()}: ifNull = ${variableValue.predicate.ifNull}, ifNotNull = ${variableValue.predicate.ifNotNull}")
                            }

            // TODO: Will it be simpler to add a PredicateHolder here?
            fun buildBooleanPredicate(expression: IrExpression): BooleanPredicate {
                val matchResultAndAnd = expression.matchAndAnd()
                if (matchResultAndAnd != null) {
                    val (left, right) = matchResultAndAnd
                    val leftBooleanPredicate = buildBooleanPredicate(left)
                    val rightBooleanPredicate = usingUpperLevelPredicate(leftBooleanPredicate.ifTrue) { buildBooleanPredicate(right) }
                    return BooleanPredicate(
                            ifTrue = Predicates.and(leftBooleanPredicate.ifTrue, rightBooleanPredicate.ifTrue),
                            ifFalse = Predicates.or(
                                    leftBooleanPredicate.ifFalse,
                                    Predicates.and(leftBooleanPredicate.ifTrue, rightBooleanPredicate.ifFalse)
                            )
                    )
                }
                val matchResultOrOr = expression.matchOrOr()
                if (matchResultOrOr != null) {
                    val (left, right) = matchResultOrOr
                    val leftBooleanPredicate = buildBooleanPredicate(left)
                    val rightBooleanPredicate = usingUpperLevelPredicate(leftBooleanPredicate.ifFalse) { buildBooleanPredicate(right) }
                    return BooleanPredicate(
                            ifTrue = Predicates.or(
                                    leftBooleanPredicate.ifTrue,
                                    Predicates.and(leftBooleanPredicate.ifFalse, rightBooleanPredicate.ifTrue)
                            ),
                            ifFalse = Predicates.and(leftBooleanPredicate.ifFalse, rightBooleanPredicate.ifFalse)
                    )
                }
                val matchResultNot = expression.matchNot()
                if (matchResultNot != null) {
                    return buildBooleanPredicate(matchResultNot).invert()
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
                            left.accept(this, Predicate.Empty).predicate
                        val rightPredicateHolder = PredicateHolder()
                        val nullablePredicate = usingUpperLevelPredicate(leftPredicate) { buildNullablePredicate(right, rightPredicateHolder) }
                        val result = Predicates.and(leftPredicate, rightPredicateHolder.predicate)
                        if (nullablePredicate == null) {
                            BooleanPredicate(
                                    ifTrue = Predicates.and(result, Predicates.disjunctionOf(ComplexTerm(expression, value = true))),
                                    ifFalse = Predicates.and(result, Predicates.disjunctionOf(ComplexTerm(expression, value = false)))
                            )
                        } else if (leftIsNullConst) {
                            BooleanPredicate(
                                    ifTrue = Predicates.and(result, nullablePredicate.ifNull),
                                    ifFalse = Predicates.and(result, nullablePredicate.ifNotNull)
                            )
                        } else {
                            BooleanPredicate(
                                    ifTrue = Predicates.and(
                                            result,
                                            Predicates.and(
                                                    nullablePredicate.ifNotNull,
                                                    Predicates.disjunctionOf(ComplexTerm(expression, value = true))
                                            )
                                    ),
                                    ifFalse = Predicates.and(
                                            result,
                                            Predicates.or(
                                                    nullablePredicate.ifNull,
                                                    Predicates.and(nullablePredicate.ifNotNull, Predicates.disjunctionOf(ComplexTerm(expression, value = false)))
                                            )
                                    )
                            )
                        }
                    } else if ((rightIsNullConst || !right.type.isNullable()) && left.type.isNullable()) {
                        val leftPredicateHolder = PredicateHolder()
                        val nullablePredicate = buildNullablePredicate(left, leftPredicateHolder)
                        val leftCommonPredicate = leftPredicateHolder.predicate
                        return if (nullablePredicate == null) {
                            val result = right.accept(this, leftCommonPredicate).predicate
                            BooleanPredicate(
                                    ifTrue = Predicates.and(result, Predicates.disjunctionOf(ComplexTerm(expression, value = true))),
                                    ifFalse = Predicates.and(result, Predicates.disjunctionOf(ComplexTerm(expression, value = false)))
                            )
                        } else if (rightIsNullConst) {
                            BooleanPredicate(
                                    ifTrue = Predicates.and(leftCommonPredicate, nullablePredicate.ifNull),
                                    ifFalse = Predicates.and(leftCommonPredicate, nullablePredicate.ifNotNull)
                            )
                        } else {
                            val leftIsNullPredicate = Predicates.and(leftCommonPredicate, nullablePredicate.ifNull)
                            val leftIsNotNullPredicate = Predicates.and(leftCommonPredicate, nullablePredicate.ifNotNull)
                            val leftPredicate = Predicates.and(
                                    leftCommonPredicate,
                                    Predicates.or(nullablePredicate.ifNull, nullablePredicate.ifNotNull)
                            )
                            val rightPredicate = usingUpperLevelPredicate(leftPredicate) { right.accept(this, Predicate.Empty).predicate }
                            val fullLeftIsNullPredicate = Predicates.and(leftIsNullPredicate, rightPredicate)
                            val fullLeftIsNotNullPredicate = Predicates.and(leftIsNotNullPredicate, rightPredicate)
                            BooleanPredicate(
                                    ifTrue = Predicates.and(
                                            fullLeftIsNotNullPredicate,
                                            Predicates.disjunctionOf(ComplexTerm(expression, value = true))
                                    ),
                                    ifFalse = Predicates.or(
                                            fullLeftIsNullPredicate,
                                            Predicates.and(
                                                    fullLeftIsNotNullPredicate,
                                                    Predicates.disjunctionOf(ComplexTerm(expression, value = false))
                                            )
                                    )
                            )
                        }
                    } else {
                        val result = expression.accept(this, Predicate.Empty).predicate
                        return BooleanPredicate(
                                ifTrue = Predicates.and(result, Predicates.disjunctionOf(ComplexTerm(expression, value = true))),
                                ifFalse = Predicates.and(result, Predicates.disjunctionOf(ComplexTerm(expression, value = false)))
                        )
                    }
                }

                if ((expression as? IrConst)?.value == true) {
                    return BooleanPredicate(ifTrue = Predicate.Empty, ifFalse = Predicate.False)
                }
                if ((expression as? IrConst)?.value == false) {
                    return BooleanPredicate(ifTrue = Predicate.False, ifFalse = Predicate.Empty)
                }
                if (expression is IrTypeOperatorCall && expression.isTypeCheck()) {
                    val (predicate, variable) = expression.argument.accept(this, Predicate.Empty)
                    return if (variable == null)
                        BooleanPredicate(
                                ifTrue = Predicates.and(Predicates.disjunctionOf(ComplexTerm(expression, value = true)), predicate),
                                ifFalse = Predicates.and(Predicates.disjunctionOf(ComplexTerm(expression, value = false)), predicate)
                        )
                    else {
                        tryOptimizeTypeCheck(expression, variable, predicate)
                        val fullIsSubtypeOfPredicate = Predicates.and(predicate, buildIsSubtypeOfPredicate(variable, expression.typeOperand))
                        val fullIsNotSubtypeOfPredicate = Predicates.and(predicate, buildIsNotSubtypeOfPredicate(variable, expression.typeOperand))
                        return if (expression.operator == IrTypeOperator.INSTANCEOF)
                            BooleanPredicate(ifTrue = fullIsSubtypeOfPredicate, ifFalse = fullIsNotSubtypeOfPredicate)
                        else
                            BooleanPredicate(ifTrue = fullIsNotSubtypeOfPredicate, ifFalse = fullIsSubtypeOfPredicate)
                    }
                }

                val (predicate, variable) = expression.accept(this, Predicate.Empty)
                return if (variable == null) {
                    BooleanPredicate(
                            ifTrue = Predicates.and(predicate, Predicates.disjunctionOf(ComplexTerm(expression, value = true))),
                            ifFalse = Predicates.and(predicate, Predicates.disjunctionOf(ComplexTerm(expression, value = false)))
                    )
                } else {
                    val variablePredicate = buildBooleanPredicate(variable)
                    return BooleanPredicate(
                            ifTrue = Predicates.and(predicate, variablePredicate.ifTrue),
                            ifFalse = Predicates.and(predicate, variablePredicate.ifFalse)
                    )
                }
            }

            fun buildNullablePredicate(variable: IrValueDeclaration): NullablePredicate =
                    variableAliases[variable]?.let { buildNullablePredicate(it) }
                            ?: when (val variableValue = variableValues[variable]) {
                                null -> {
                                    NullablePredicate(
                                            ifNull = Predicates.disjunctionOf(SimpleTerm.IsNull(variable)),
                                            ifNotNull = Predicates.disjunctionOf(SimpleTerm.IsNotNull(variable))
                                    )
                                }
                                is VariableValue.NullablePredicate -> variableValue.predicate
                                is VariableValue.BooleanPredicate -> error("Unexpected boolean predicate for ${variable.render()}")
                            }

            private fun buildNullablePredicate(expression: IrExpression, predicateHolder: PredicateHolder): NullablePredicate? {
                if (!expression.type.isNullable()) {
                    predicateHolder.predicate = expression.accept(this, Predicate.Empty).predicate
                    return NullablePredicate(ifNull = Predicate.False, ifNotNull = Predicate.Empty)
                }
                if (expression is IrTypeOperatorCall && expression.operator == IrTypeOperator.SAFE_CAST) {
                    val (predicate, variable) = expression.argument.accept(this, Predicate.Empty)
                    predicateHolder.predicate = predicate
                    return if (variable == null) {
                        null
                    } else {
                        tryOptimizeTypeCheck(expression, variable, predicate)
                        return NullablePredicate(
                                ifNull = buildIsNotSubtypeOfPredicate(variable, expression.typeOperand),
                                ifNotNull = buildIsSubtypeOfPredicate(variable, expression.typeOperand)
                        )
                    }
                }
                val matchResultSafeCall = expression.matchSafeCall()
                if (matchResultSafeCall != null) {
                    val (safeReceiverInitializer, result) = matchResultSafeCall
                    val safeReceiverPredicate = buildNullablePredicate(safeReceiverInitializer, predicateHolder)
                    return if (safeReceiverPredicate == null) {
                        null
                    } else {
                        NullablePredicate(
                                ifNull = safeReceiverPredicate.ifNull,
                                ifNotNull = usingUpperLevelPredicate(predicateHolder.predicate) {
                                    result.accept(this, safeReceiverPredicate.ifNotNull).predicate
                                }
                        )
                    }
                }
                val (predicate, variable) = expression.accept(this, Predicate.Empty)
                predicateHolder.predicate = predicate
                return variable?.let { buildNullablePredicate(it) }
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

            override fun visitElement(element: IrElement, data: Predicate): VisitorResult {
                val children = element.getImmediateChildren()
                var predicate = data
                for (child in children)
                    predicate = child.accept(this, predicate).predicate
                return VisitorResult(predicate, null)
            }

            override fun visitBlock(expression: IrBlock, data: Predicate): VisitorResult {
                val returnableBlock = expression as? IrReturnableBlock
                val statements = expression.statements
                if (returnableBlock == null) {
                    var predicate = data
                    var resultVariable: IrValueDeclaration? = null
                    statements.forEachIndexed { index, statement ->
                        val result = statement.accept(this, predicate)
                        predicate = result.predicate
                        if (index == statements.lastIndex && expression.type != unitType)
                            resultVariable = result.resultVariable
                    }
                    return VisitorResult(predicate, resultVariable)
                }

                val cfmpInfo = ControlFlowMergePointInfo(upperLevelPredicates.size)
                returnableBlockCFMPInfos[returnableBlock] = cfmpInfo
                super.visitBlock(expression, data)
                returnableBlockCFMPInfos.remove(returnableBlock)

                return finishControlFlowMerging(expression, cfmpInfo)
            }

            override fun visitReturn(expression: IrReturn, data: Predicate): VisitorResult {
                val result = expression.value.accept(this, data)
                val returnableBlock = expression.returnTargetSymbol.owner as? IrReturnableBlock
                if (returnableBlock != null) {
                    val cfmpInfo = returnableBlockCFMPInfos[returnableBlock]!!
                    if (result.predicate != Predicate.False)
                        controlFlowMergePoint(cfmpInfo, result)
                    if (debugOutput) {
                        println("QXX: ${expression.dump()}")
                        println("    result = ${cfmpInfo.predicate}")
                    }
                }
                return VisitorResult.Nothing
            }

            override fun visitBreak(jump: IrBreak, data: Predicate): VisitorResult {
                val cfmpInfo = breaksCFMPInfos[jump.loop]!!
                controlFlowMergePoint(cfmpInfo, VisitorResult(data, null))

                return VisitorResult.Nothing
            }

            override fun visitContinue(jump: IrContinue, data: Predicate): VisitorResult {
                val cfmpInfo = continuesCFMPInfos[jump.loop]!!
                controlFlowMergePoint(cfmpInfo, VisitorResult(data, null))

                return VisitorResult.Nothing
            }

            override fun visitLoop(loop: IrLoop, data: Predicate): VisitorResult = usingUpperLevelPredicate(data) {
                val startPredicate = if (loop is IrWhileLoop)
                    buildBooleanPredicate(loop.condition)
                else BooleanPredicate(ifTrue = Predicate.Empty, ifFalse = Predicate.False)

                val breaksCFMPInfo = ControlFlowMergePointInfo(upperLevelPredicates.size)
                breaksCFMPInfos[loop] = breaksCFMPInfo
                var predicateBeforeBody = startPredicate.ifTrue
                for (iter in 0..<10) {
                    val savedReturnableBlockCFMPInfos = mutableMapOf<IrReturnableBlock, ControlFlowMergePointInfo>()
                    val savedBreaksCFMPInfos = mutableMapOf<IrLoop, ControlFlowMergePointInfo>()
                    val savedContinuesCFMPInfos = mutableMapOf<IrLoop, ControlFlowMergePointInfo>()
                    returnableBlockCFMPInfos.forEach { (key, cfmpInfo) ->
                        savedReturnableBlockCFMPInfos[key] = ControlFlowMergePointInfo(cfmpInfo)
                    }
                    breaksCFMPInfos.forEach { (key, cfmpInfo) ->
                        savedBreaksCFMPInfos[key] = ControlFlowMergePointInfo(cfmpInfo)
                    }
                    continuesCFMPInfos.forEach { (key, cfmpInfo) ->
                        savedContinuesCFMPInfos[key] = ControlFlowMergePointInfo(cfmpInfo)
                    }

                    val body = loop.body
                    val predicateAfterBody = if (body == null)
                        predicateBeforeBody
                    else {
                        val continuesCFMPInfo = ControlFlowMergePointInfo(upperLevelPredicates.size)
                        continuesCFMPInfos[loop] = continuesCFMPInfo
                        val bodyResult = body.accept(this, predicateBeforeBody)
                        continuesCFMPInfos.remove(loop)
                        controlFlowMergePoint(continuesCFMPInfo, VisitorResult(bodyResult.predicate, null))
                        finishControlFlowMerging(body, continuesCFMPInfo).predicate
                    }
                    val conditionPredicate = usingUpperLevelPredicate(predicateAfterBody) { buildBooleanPredicate(loop.condition) }
                    predicateBeforeBody = Predicates.and(predicateAfterBody, conditionPredicate.ifTrue)
                    controlFlowMergePoint(breaksCFMPInfo, VisitorResult(Predicates.and(predicateAfterBody, conditionPredicate.ifFalse), null))

                    var somethingChanged = false
                    for ((key, cfmpInfo) in returnableBlockCFMPInfos) {
                        if (!savedReturnableBlockCFMPInfos[key]!!.contentEquals(cfmpInfo)) {
                            somethingChanged = true
                            break
                        }
                    }
                    if (!somethingChanged) {
                        for ((key, cfmpInfo) in breaksCFMPInfos) {
                            if (!savedBreaksCFMPInfos[key]!!.contentEquals(cfmpInfo)) {
                                somethingChanged = true
                                break
                            }
                        }
                    }
                    if (!somethingChanged) {
                        for ((key, cfmpInfo) in continuesCFMPInfos) {
                            if (!savedContinuesCFMPInfos[key]!!.contentEquals(cfmpInfo)) {
                                somethingChanged = true
                                break
                            }
                        }
                    }

                    if (!somethingChanged) break
                }

                val result = finishControlFlowMerging(loop, breaksCFMPInfo)
                VisitorResult(Predicates.and(data, Predicates.or(startPredicate.ifFalse, result.predicate)), null)
            }

            fun IrTypeOperatorCall.isCast() =
                    operator == IrTypeOperator.CAST || operator == IrTypeOperator.IMPLICIT_CAST

            fun IrTypeOperatorCall.isTypeCheck() =
                    operator == IrTypeOperator.INSTANCEOF || operator == IrTypeOperator.NOT_INSTANCEOF

            fun tryOptimizeTypeCheck(expression: IrTypeOperatorCall, variable: IrValueDeclaration, predicate: Predicate) {
                val fullPredicate = getFullPredicate(predicate, true, 0)
                if (debugOutput) {
                    println("ZZZ: ${expression.dump()}")
                    println("    $fullPredicate")
                }
                // Check if (predicate & (v !is T)) is identically equal to false: meaning the cast will always succeed.
                // Similarly, if (predicate & (v is T)) is identically equal to false, then the cast will never succeed.
                // Note: further improvement will be to check not only for identical equality to false but actually try to
                // find the combination of leaf terms satisfying the predicate (though it can be computationally unfeasible).
                val castIsFailedPredicate = Predicates.and(fullPredicate, buildIsNotSubtypeOfPredicate(variable, expression.typeOperand))
                if (debugOutput) {
                    println("    castIsFailedPredicate: $castIsFailedPredicate")
                }
                if (castIsFailedPredicate == Predicate.False) {
                    // The cast will always succeed.
                    val result = when (typeCheckResults[expression]) {
                        null, TypeCheckResult.ALWAYS_SUCCEEDS -> TypeCheckResult.ALWAYS_SUCCEEDS
                        else -> TypeCheckResult.UNKNOWN
                    }
                    typeCheckResults[expression] = result
                    //typeCheckResults[expression] = true
                }
                val castIsSuccessfulPredicate = Predicates.and(fullPredicate, buildIsSubtypeOfPredicate(variable, expression.typeOperand))
                if (debugOutput) {
                    println("    castIsSuccessfulPredicate: $castIsSuccessfulPredicate")
                    println()
                }
                if (castIsSuccessfulPredicate == Predicate.False) {
                    // The cast will never succeed.
                    val result = when (typeCheckResults[expression]) {
                        null, TypeCheckResult.NEVER_SUCCEEDS -> TypeCheckResult.NEVER_SUCCEEDS
                        else -> TypeCheckResult.UNKNOWN
                    }
                    typeCheckResults[expression] = result
                    //typeCheckResults[expression] = false
                }
            }

            override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Predicate): VisitorResult {
                /*
                  TYPE_OP type=<root>.A origin=IMPLICIT_CAST typeOperand=<root>.A
                    TYPE_OP type=<root>.A? origin=CAST typeOperand=<root>.A?
                      TYPE_OP type=kotlin.Any origin=IMPLICIT_CAST typeOperand=kotlin.Any
                        GET_VAR 'x: kotlin.Any declared in <root>.foo' type=kotlin.Any origin=null
                 */
                val (argumentPredicate, argumentVariable) = expression.argument.accept(this, data)
                if (expression.isCast() || expression.isTypeCheck() || expression.operator == IrTypeOperator.SAFE_CAST) {
                    if (argumentVariable != null) {
                        tryOptimizeTypeCheck(expression, argumentVariable, argumentPredicate)

                        return if (expression.isCast())
                            VisitorResult(
                                    Predicates.and(argumentPredicate, buildIsSubtypeOfPredicate(argumentVariable, expression.typeOperand)),
                                    argumentVariable
                            )
                        else VisitorResult(argumentPredicate, null)
                    }
//                    if (debugOutput) {
//                        println()
//                    }
                }
                return VisitorResult(argumentPredicate, null)
            }

            override fun visitWhen(expression: IrWhen, data: Predicate): VisitorResult = usingUpperLevelPredicate(data) {
                val cfmpInfo = ControlFlowMergePointInfo(upperLevelPredicates.size)
                var predicate: Predicate = Predicate.Empty
                for (branch in expression.branches) {
                    usingUpperLevelPredicate(predicate) {
                        val conditionBooleanPredicate = buildBooleanPredicate(branch.condition)
                        if (debugOutput) {
                            println("QXX: ${branch.condition.dump()}")
                            println("    upperLevelPredicate = ${getFullPredicate(Predicate.Empty, false, 0)}")
                            println("    condition = ${conditionBooleanPredicate.ifTrue}")
                            println("    ~condition = ${conditionBooleanPredicate.ifFalse}")
                            println("    result = ${cfmpInfo.predicate}")
                            println()
                        }
                        val savedVariableAliases = variableAliases.toMap()
                        val branchResult = branch.result.accept(this, conditionBooleanPredicate.ifTrue)
                        if (branchResult.predicate != Predicate.False) { // The result is not unreachable.
                            controlFlowMergePoint(cfmpInfo, branchResult)
                        }
                        variableAliases.clear()
                        for ((variable, alias) in savedVariableAliases)
                            variableAliases[variable] = alias
                        predicate = Predicates.and(predicate, conditionBooleanPredicate.ifFalse)
                    }
                }
                if (debugOutput) {
                    println("QXX")
                    println("    result = ${cfmpInfo.predicate}")
                    println("    predicate = $predicate")
                }
                if (!expression.branches.last().isUnconditional()) // Non-exhaustive when.
                    controlFlowMergePoint(cfmpInfo, VisitorResult(predicate, null))
                if (debugOutput) {
                    println("    result = ${cfmpInfo.predicate}")
                }

                val result = finishControlFlowMerging(expression, cfmpInfo)
                if (debugOutput)
                    println("    result = ${result.predicate}")
                val resultPredicate = Predicates.and(data, result.predicate)
                if (debugOutput) {
                    println("    result = $resultPredicate")
                    println()
                }
                VisitorResult(resultPredicate, result.resultVariable)
            }

            fun setVariable(variable: IrVariable, value: IrExpression, data: Predicate): Predicate {
                val actualVariable = if (variable.isMutable)
                    createPhantomVariable(variable, value).also { variableAliases[variable] = it }
                else variable
                return if (variable.type.isBoolean()) {
                    val booleanPredicate = usingUpperLevelPredicate(data) { buildBooleanPredicate(value) }
// TODO:
//                    val (ifTrue, ifFalse) = booleanPredicate
//                    val optimizedIfTrue = Predicates.optimizeAwayComplexTerms(ifTrue)
//                    val optimizedIfFalse = Predicates.optimizeAwayComplexTerms(ifFalse)
//                    val optimizedBooleanPredicate = BooleanPredicate(
//                            ifTrue = if (optimizedIfTrue == Predicate.Empty && ifTrue != Predicate.Empty)
//                                Predicates.disjunctionOf(ComplexTerm(value, true))
//                            else optimizedIfTrue,
//                            ifFalse = if (optimizedIfFalse == Predicate.Empty && ifFalse != Predicate.Empty)
//                                Predicates.disjunctionOf(ComplexTerm(value, false))
//                            else optimizedIfFalse
//                    )
                    variableValues[actualVariable] = VariableValue.BooleanPredicate(booleanPredicate)
                    Predicates.and(data, Predicates.or(booleanPredicate.ifTrue, booleanPredicate.ifFalse))
                } else if (variable.type.isNullable()) {
                    val predicateHolder = PredicateHolder()
                    val nullablePredicate = usingUpperLevelPredicate(data) { buildNullablePredicate(value, predicateHolder) }
                    val result = Predicates.and(data, predicateHolder.predicate)
                    if (nullablePredicate == null)
                        result
                    else {
// TODO:
//                        val (ifNull, ifNotNull) = nullablePredicate
//                        val optimizedIfNull = Predicates.optimizeAwayComplexTerms(ifNull)
//                        val optimizedIfNotNull = Predicates.optimizeAwayComplexTerms(ifNotNull)
//                        val optimizedNullablePredicate = NullablePredicate(
//                                ifNull = if (optimizedIfNull == Predicate.Empty && ifNull != Predicate.Empty)
//                                    Predicates.disjunctionOf(ComplexTerm(value, true))
//                                else optimizedIfNull,
//                                ifNotNull = if (optimizedIfNotNull == Predicate.Empty && ifNotNull != Predicate.Empty)
//                                    Predicates.disjunctionOf(ComplexTerm(value, false))
//                                else optimizedIfNotNull
//                        )
                        variableValues[actualVariable] = VariableValue.NullablePredicate(nullablePredicate)
                        Predicates.and(result, Predicates.or(nullablePredicate.ifNull, nullablePredicate.ifNotNull))
                    }
                } else {
                    val (predicate, delegatedVariable) = value.accept(this, data)
                    if (delegatedVariable != null)
                        variableAliases[variable] = variableAliases[delegatedVariable] ?: delegatedVariable
                    predicate
                }
            }

            override fun visitVariable(declaration: IrVariable, data: Predicate): VisitorResult {
                val initializer = declaration.initializer
                val resultPredicate = if (initializer == null) data else setVariable(declaration, initializer, data)
                return VisitorResult(resultPredicate, null)
            }

            override fun visitSetValue(expression: IrSetValue, data: Predicate): VisitorResult {
                val variable = expression.symbol.owner as? IrVariable ?: error("Unexpected set to ${expression.symbol.owner.render()}")
                return VisitorResult(setVariable(variable, expression.value, data), null)
            }

            override fun visitGetValue(expression: IrGetValue, data: Predicate): VisitorResult {
                return VisitorResult(data, expression.symbol.owner)
            }

        }, Predicate.Empty)

        if (typeCheckResults.isEmpty()) return
        val irBuilder = context.createIrBuilder(container.symbol)
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
                expression.transformChildrenVoid()

                val typeCheckResult = when(typeCheckResults[expression]) {
                    null, TypeCheckResult.UNKNOWN -> return expression
                    TypeCheckResult.ALWAYS_SUCCEEDS -> true
                    TypeCheckResult.NEVER_SUCCEEDS -> false
                }
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

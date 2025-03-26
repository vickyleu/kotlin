/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.imports.Importable
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.ir.generator.*
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.ir.generator.model.symbol.symbolVisitorMethodName
import org.jetbrains.kotlin.utils.withIndent

internal class IrTreeSymbolsVisitorPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractVisitorPrinter<Element, Field>(printer) {
    override val visitorTypeParameters: List<TypeVariable>
        get() = emptyList()

    override val visitorDataType: TypeRef
        get() = StandardTypes.nothing.copy(nullable = true)

    override fun visitMethodReturnType(element: Element): TypeRef = StandardTypes.unit

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(irVisitorVoidType)

    override val optIns: List<ClassRef<*>> = listOf(irImplementationDetailType)

    override val implementationKind: ImplementationKind
        get() = ImplementationKind.OpenClass

    private val symbolVisitorParameter = PrimaryConstructorParameter(
        FunctionParameter("symbolVisitor", symbolVisitorType),
        VariableKind.VAL,
        Visibility.PRIVATE
    )

    override val constructorParameters: List<PrimaryConstructorParameter> = listOf(symbolVisitorParameter)

    override fun ImportCollectingPrinter.printAdditionalMethods() {
        println()
        println("private fun ${irTypeType.render()}.visitType() { symbolVisitor.visitType(this) }")
    }

    override fun printMethodsForElement(element: Element) {
        if (!element.generateVisitorMethod) return

        printer.run {
            if (element.isRootElement) {
                println()
                printVisitMethodDeclaration(element, hasDataParameter = false, override = true)
                println(" =")
                withIndent {
                    println("throw IllegalArgumentException(\"Unsupported element type: $${element.visitorParameterName}\")")
                }
                return
            }

            if (element.implementations.isEmpty()) return

            println()
            printVisitMethodDeclaration(element, hasDataParameter = false, override = true)

            printBlock {
                val implementation = element.implementations.singleOrNull() ?: error("Ambiguous implementation")
                for (field in implementation.allFields) {
                    visitField(element, field)
                }
                println(element.visitorParameterName, ".acceptChildren(this, data = null)")
            }
        }
    }

    private fun ImportCollectingPrinter.visitField(element: Element, field: Field) {
        val typeRef = if (field is ListField) {
            field.baseType
        } else {
            field.typeRef
        } as? ClassOrElementRef ?: return

        if (!hasSomethingToVisit(field, typeRef)) return

        if (field is ListField) {
            val safeCall = if (field.typeRef.nullable) "?." else "."
            print(element.visitorParameterName, ".", field.name, safeCall, "forEach { ")
            visitValue(field, typeRef, "it")
            print(" }")
        } else {
            visitValue(field, typeRef, element.visitorParameterName, ".", field.name)
        }
        println()
    }

    private fun hasSomethingToVisit(field: Field, typeRef: ClassOrElementRef): Boolean {
        return field.symbolClass != null ||
                typeRef.isSameClassAs(irTypeType) ||
                typeRef.isSameClassAs(type<ValueClassRepresentation<*>>())
    }

    private fun ImportCollectingPrinter.visitValue(field: Field, typeRef: ClassOrElementRef, vararg valueArgs: Any?) {
        val symbolFieldClass = field.symbolClass
        val safeCall = if (typeRef.nullable) "?." else "."
        when {
            symbolFieldClass != null -> {
                val symbolVisitFunction =
                    symbolVisitorMethodName(symbolFieldClass, field.symbolFieldRole ?: AbstractField.SymbolFieldRole.REFERENCED)
                if (typeRef.nullable) {
                    print(*valueArgs, "?.let(", symbolVisitorParameter.name, "::", symbolVisitFunction, ")")
                } else {
                    print(symbolVisitorParameter.name, ".", symbolVisitFunction, "(", *valueArgs, ")")
                }
            }
            typeRef.isSameClassAs(irTypeType) -> {
                print(*valueArgs, safeCall, "visitType()")
            }
            typeRef.isSameClassAs(type<ValueClassRepresentation<*>>()) -> {
                print(*valueArgs, safeCall, "mapUnderlyingType { it.visitType(); it }")
            }
        }
    }
}

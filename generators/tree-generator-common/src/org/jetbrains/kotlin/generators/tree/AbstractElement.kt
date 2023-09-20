/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

/**
 * A common interface representing a FIR or IR tree element.
 */
interface AbstractElement<Element, Field> : ElementOrRef<Element, Field>, FieldContainer, ImplementationKindOwner
        where Element : AbstractElement<Element, Field>,
              Field : AbstractField {

    val name: String

    val kDoc: String?

    val fields: Set<Field>

    val params: List<TypeVariable> // TODO: Rename to `typeParameters` (rn this name would clash with the extension function)

    val elementParents: List<ElementRef<Element, Field>>

    val otherParents: MutableList<ClassRef<*>>

    val parentRefs: List<ClassOrElementRef>
        get() = elementParents + otherParents

    val isRootElement: Boolean
        get() = elementParents.isEmpty()

    val isSealed: Boolean
        get() = false

    /**
     * The name of the method in visitors used to visit this element.
     */
    val visitFunctionName: String

    /**
     * The name of the parameter representing this element in the visitor method used to visit this element.
     */
    val visitorParameterName: String

    /**
     * The default element to visit if the method for visiting this element is not overridden.
     */
    val parentInVisitor: Element?

    override val allParents: List<Element>
        get() = elementParents.map { it.element }

    override fun getTypeWithArguments(notNull: Boolean): String = type + generics

    override val allFields: List<Field>

    override val walkableChildren: List<Field>

    override val transformableChildren: List<Field>

    override fun get(fieldName: String): Field? {
        return allFields.firstOrNull { it.name == fieldName }
    }
}

val AbstractElement<*, *>.generics: String
    get() = params.takeIf { it.isNotEmpty() }
        ?.let { it.joinToString(", ", "<", ">") { it.name } }
        ?: ""

fun AbstractElement<*, *>.typeParameters(end: String = ""): String = params.takeIf { it.isNotEmpty() }
    ?.joinToString(", ", "<", ">$end") { param ->
        param.name + (param.bounds.singleOrNull()?.let { " : ${it.typeWithArguments}" } ?: "")
    } ?: ""

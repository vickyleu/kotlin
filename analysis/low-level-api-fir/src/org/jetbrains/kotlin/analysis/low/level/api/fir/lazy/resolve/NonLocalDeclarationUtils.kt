/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal fun elementCanBeLazilyResolved(element: KtElement?, codeFragmentAware: Boolean = false): Boolean = when (element) {
    null -> false
    is KtFunctionLiteral -> false
    is KtTypeParameter -> elementCanBeLazilyResolved(element.parentOfType<KtNamedDeclaration>(withSelf = false), codeFragmentAware)
    is KtScript -> elementCanBeLazilyResolved(element.parent as? KtFile, codeFragmentAware)
    is KtFile -> codeFragmentAware || element !is KtCodeFragment
    is KtDestructuringDeclarationEntry -> elementCanBeLazilyResolved(element.parent as? KtDestructuringDeclaration, codeFragmentAware)
    is KtParameter -> elementCanBeLazilyResolved(element.ownerDeclaration, codeFragmentAware)
    is KtCallableDeclaration, is KtEnumEntry, is KtDestructuringDeclaration, is KtAnonymousInitializer -> {
        val parentToCheck = when (val parent = element.parent) {
            is KtClassOrObject, is KtFile -> parent
            is KtClassBody -> parent.parent as? KtClassOrObject
            is KtBlockExpression -> parent.parent as? KtScript
            else -> null
        }

        elementCanBeLazilyResolved(parentToCheck.takeUnless { it is KtEnumEntry }, codeFragmentAware)
    }

    is KtPropertyAccessor -> elementCanBeLazilyResolved(element.property, codeFragmentAware)
    is KtClassOrObject -> element.isTopLevel() || element.getClassId() != null
    is KtTypeAlias -> element.isTopLevel() || element.getClassId() != null
    is KtModifierList -> element.isNonLocalDanglingModifierList()
    !is KtNamedDeclaration -> false
    else -> errorWithAttachment("Unexpected ${element::class}") {
        withPsiEntry("declaration", element)
    }
}

/**
 * Detects a common pattern of invalid code where a modifier list (e.g., annotation)
 * is dangling—unattached to a valid declaration—or left unclosed and followed by another declaration.
 *
 * ### Examples
 *
 * ```kotlin
 * class C1 {
 *     @Ann1 @Ann2
 * }
 *
 * class C2 {
 *     @Ann(
 *     fun foo() {}
 * }
 *
 * @Ann("argument"
 * fun foo() {}
 * ```
 * @see org.jetbrains.kotlin.fir.declarations.FirDanglingModifierList
 */
private fun KtModifierList.isNonLocalDanglingModifierList(): Boolean {
    val parent = parent

    if (parent !is KtFile && parent !is KtClassBody) {
        // We recognize only malformed PSI, where the modifier list is not attached
        // to some annotated declaration (it is not a child of this declaration).
        //
        // Here is an example of a well-formed PSI, where the modifier list is a child of the interface,
        // even though the PSI contains error elements (inside brackets).
        // This is not considered a dangling modifier list:
        //
        // @file:[]
        // interface I
        return false
    }

    if (parent is KtClassBody && (parent.parent as? KtClassOrObject)?.isLocal() == true) {
        // We ignore local modifier lists for LL file structure purposes
        return false
    }

    // A dangling modifier list is, by definition, syntactically invalid.
    // Given the variety of invalid code patterns, we rely on a simple best-effort check:
    // a modifier list is considered dangling if it's followed by a syntax error
    // or contains any syntax errors within its descendants recursively.
    return getNextSiblingIgnoringWhitespaceAndComments() is PsiErrorElement || anyDescendantOfType<PsiErrorElement>()
}

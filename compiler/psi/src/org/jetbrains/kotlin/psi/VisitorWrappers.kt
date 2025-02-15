/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

fun typeReferenceRecursiveVisitor(block: (KtTypeReference) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitTypeReference(typeReference: KtTypeReference) {
            super.visitTypeReference(typeReference)
            block(typeReference)
        }
    }

fun declarationRecursiveVisitor(block: (KtDeclaration) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitDeclaration(declaration: KtDeclaration) {
            super.visitDeclaration(declaration)
            block(declaration)
        }
    }

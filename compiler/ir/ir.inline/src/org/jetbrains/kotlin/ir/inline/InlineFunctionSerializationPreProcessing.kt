/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.util.erasedTopLevelCopy
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.parentsWithSelf
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

@Suppress("unused")
class InlineFunctionSerializationPreProcessing(private val context: LoweringContext) : IrVisitorVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        if (!declaration.isInline || declaration.body == null || declaration.isEffectivelyPrivate()) return
        declaration.erasedTopLevelCopy = declaration.eraseTypeParameters().convertToTopLevel()
    }

    private fun IrSimpleFunction.eraseTypeParameters(): IrSimpleFunction {
        val typeArguments = parentsWithSelf.flatMap { (it as? IrTypeParametersContainer)?.typeParameters ?: emptyList() }
            .associate { it.symbol to (if (it.isReified) null else context.irBuiltIns.anyNType) }
        return InlineFunctionBodyPreprocessor(typeArguments, parent).preprocess(this) as IrSimpleFunction
    }

    private fun IrSimpleFunction.convertToTopLevel(): IrSimpleFunction {
        parameters.forEach {
            it.kind = IrParameterKind.Regular
        }

        correspondingPropertySymbol = null
        parent = file

        return this
    }
}
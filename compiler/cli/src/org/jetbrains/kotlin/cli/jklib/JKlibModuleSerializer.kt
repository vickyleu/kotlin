/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib

import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.jvm.serialization.JvmGlobalDeclarationTable
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrFile

class JKlibModuleSerializer (
    diagnosticReporter: IrDiagnosticReporter,
    compatibilityMode: CompatibilityMode, normalizeAbsolutePaths: Boolean, sourceBaseDirs: Collection<String>,
    shouldCheckSignaturesOnUniqueness: Boolean,
    private val languageVersionSettings: LanguageVersionSettings,
    private val bodiesOnlyForInlines: Boolean,
    private val publicAbiOnly: Boolean
) : IrModuleSerializer<IrFileSerializer>(
    diagnosticReporter, compatibilityMode, normalizeAbsolutePaths, sourceBaseDirs,
    shouldCheckSignaturesOnUniqueness
) {

    override val globalDeclarationTable = JvmGlobalDeclarationTable()

    override fun createSerializerForFile(file: IrFile): IrFileSerializer =
        IrFileSerializer(
            DeclarationTable(globalDeclarationTable),
            compatibilityMode = compatibilityMode,
            languageVersionSettings = languageVersionSettings,
            bodiesOnlyForInlines = bodiesOnlyForInlines,
            normalizeAbsolutePaths = normalizeAbsolutePaths,
            publicAbiOnly = publicAbiOnly,
            sourceBaseDirs = sourceBaseDirs
        )

}
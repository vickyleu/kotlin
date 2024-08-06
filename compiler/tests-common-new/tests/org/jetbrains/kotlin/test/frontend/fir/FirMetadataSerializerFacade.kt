/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.cli.metadata.buildKotlinMetadataLibrary
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.serialization.FirKLibSerializerExtension
import org.jetbrains.kotlin.fir.serialization.serializeSingleFirFile
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.metadata.KlibMetadataHeaderFlags
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.KlibBasedEnvironmentConfiguratorUtils

class FirMetadataSerializerFacade(
    testServices: TestServices
) : Frontend2BinaryConverter<FirOutputArtifact, BinaryArtifacts.KLib>(
    testServices,
    FrontendKinds.FIR,
    ArtifactKinds.KLib
) {
    override fun transform(module: TestModule, inputArtifact: FirOutputArtifact): BinaryArtifacts.KLib? {

        val fragments = mutableMapOf<String, MutableList<ByteArray>>()
        val languageVersionSettings = module.languageVersionSettings
        val compilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val metadataVersion = compilerConfiguration.get(CommonConfigurationKeys.METADATA_VERSION) as? BuiltInsBinaryVersion
            ?: BuiltInsBinaryVersion.INSTANCE

        for (output in inputArtifact.partsForDependsOnModules) {
            val session = output.session
            val scopeSession = output.firAnalyzerFacade.scopeSession
            val fir = output.firFiles

            for ((_, firFile: FirFile) in fir) {
                val packageFragment = serializeSingleFirFile(
                    firFile,
                    session,
                    scopeSession,
                    actualizedExpectDeclarations = null,
                    FirKLibSerializerExtension(
                        session, scopeSession, session.firProvider, metadataVersion, constValueProvider = null,
                        allowErrorTypes = false, exportKDoc = false,
                        additionalMetadataProvider = null
                    ),
                    languageVersionSettings,
                )
                fragments.getOrPut(firFile.packageFqName.asString()) { mutableListOf() }.add(packageFragment.toByteArray())
            }
        }

        val header = KlibMetadataProtoBuf.Header.newBuilder()
        header.moduleName = inputArtifact.partsForDependsOnModules.last().session.moduleData.name.asString()

        if (languageVersionSettings.isPreRelease()) {
            header.flags = KlibMetadataHeaderFlags.PRE_RELEASE
        }

        val fragmentNames = mutableListOf<String>()
        val fragmentParts = mutableListOf<List<ByteArray>>()

        for ((fqName, fragment) in fragments.entries.sortedBy { it.key }) {
            fragmentNames += fqName
            fragmentParts += fragment
            header.addPackageFragmentName(fqName)
        }

        val moduleMetadata = header.build().toByteArray()

        val serializedMetadata = SerializedMetadata(moduleMetadata, fragmentParts, fragmentNames)
        val diagnosticReporter = DiagnosticReporterFactory.createReporter()

        val outputArtifact = BinaryArtifacts.KLib(ConfUtils.getKlibArtifactFile(testServices, module.name), diagnosticReporter)
        buildKotlinMetadataLibrary(compilerConfiguration, serializedMetadata, outputArtifact.outputFile)

        return  outputArtifact
    }

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return super.shouldRunAnalysis(module)
    }
}

private object ConfUtils : KlibBasedEnvironmentConfiguratorUtils

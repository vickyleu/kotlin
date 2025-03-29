/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.IrDeserializationSettings.DeserializeFunctionBodies
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.KlibDeserializedContainerSource
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile

class NonLinkingIrInlineFunctionDeserializer(
    private val irBuiltIns: IrBuiltIns,
    private val signatureComputer: PublicIdSignatureComputer,
) {
    private val irInterner = IrInterningService()
    private val irFactory get() = irBuiltIns.irFactory

    /**
     * This is a separate symbol table ("detached") from the symbol table ("main") that is used in IR linker.
     *
     * The goal is to separate the linkage process, which should end with all symbols been bound to the respective declarations,
     * and the process of partial deserialization of inline functions, which should produce some amount of unbound symbols
     * that are not supposed to be linked and therefore should not be tracked in the main symbol table.
     */
    private val detachedSymbolTable = SymbolTable(signaturer = null, irFactory)

    private val moduleDeserializers = hashMapOf<KotlinLibrary, ModuleDeserializer>()

    fun deserializeInlineFunction(function: IrFunction): IrFunction? {
        check(function.isInline) { "Non-inline function: ${function.render()}" }
        check(!function.isFakeOverride) { "Deserialization of fake overrides is not supported: ${function.render()}" }

        if (function.body != null) return null

        check(!function.isEffectivelyPrivate()) { "Deserialization of private inline functions is not supported: ${function.render()}" }

        val deserializedContainerSource = function.containerSource
        check(deserializedContainerSource is KlibDeserializedContainerSource) {
            "Cannot deserialize inline function from a non-Kotlin library: ${function.render()}\nFunction source: " +
                    deserializedContainerSource?.let { "${it::class.java}, ${it.presentableString}" }
        }

        val library = deserializedContainerSource.klib
        val moduleDeserializer = moduleDeserializers.getOrPut(library) { ModuleDeserializer(library) }

        val functionSignature: IdSignature = signatureComputer.computeSignature(function)
        val deserializedFunction: IrFunction = moduleDeserializer.getTopLevelDeclarationOrNull(functionSignature) ?: return null

        deserializedFunction.attributeOwnerId = function
        return deserializedFunction
    }

    private fun referencePublicSymbol(signature: IdSignature, symbolKind: BinarySymbolData.SymbolKind) =
        referenceDeserializedSymbol(detachedSymbolTable, fileSymbol = null, symbolKind, signature)

    private inner class ModuleDeserializer(library: KotlinLibrary) {
        init {
            check(library.hasIr) { "Ir-less library: ${library.libraryFile.path}" }
        }

        private val fileDeserializers = (0 until library.fileCount()).map { fileIndex ->
            FileDeserializer(library, fileIndex)
        }

        fun getTopLevelDeclarationOrNull(topLevelSignature: IdSignature): IrFunction? =
            fileDeserializers.firstNotNullOfOrNull { it.getTopLevelDeclarationOrNull(topLevelSignature) }
    }

    private inner class FileDeserializer(library: KotlinLibrary, fileIndex: Int) {
        private val fileProto = ProtoFile.parseFrom(library.file(fileIndex).codedInputStream, ExtensionRegistryLite.newInstance())
        private val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(library, fileIndex))

        private val dummyFileSymbol = IrFileSymbolImpl().apply {
            val fileEntry = library.fileEntry(fileProto, fileIndex)
            IrFileImpl(
                fileEntry = NaiveSourceBasedFileEntryImpl(fileEntry.name, fileEntry.lineStartOffsetList.toIntArray()),
                symbol = this,
                packageFqName = FqName(irInterner.string(fileReader.deserializeFqName(fileProto.fqNameList)))
            )
        }

        private val symbolDeserializer = IrSymbolDeserializer(
            detachedSymbolTable,
            fileReader,
            dummyFileSymbol,
            enqueueLocalTopLevelDeclaration = {},
            irInterner,
            deserializePublicSymbol = ::referencePublicSymbol
        )

        private val declarationDeserializer = IrDeclarationDeserializer(
            builtIns = irBuiltIns,
            symbolTable = detachedSymbolTable,
            irFactory = irFactory,
            libraryFile = fileReader,
            parent = dummyFileSymbol.owner,
            settings = IrDeserializationSettings(
                deserializeFunctionBodies = DeserializeFunctionBodies.ONLY_INLINE,
                useNullableAnyAsAnnotationConstructorCallType = true,
            ),
            symbolDeserializer = symbolDeserializer,
            onDeserializedClass = { _, _ -> },
            needToDeserializeFakeOverrides = { false },
            specialProcessingForMismatchedSymbolKind = null,
            irInterner = irInterner,
        )

        private val originalSignatureToPreprocessed: Map<Int, Int> =
            fileProto.originalToPreprocessedInlineFunctionsList.filterIndexed { index, _ -> index % 2 == 0 }
                .zip(fileProto.originalToPreprocessedInlineFunctionsList.filterIndexed { index, _ -> index % 2 != 0 })
                .toMap()

        /**
         * Deserialize declarations only on demand. Cache top-level declarations to avoid repetitive deserialization
         * if the declaration happens to have multiple inline functions.
         */
        private val indexWithLazyValues: Map<IdSignature, Lazy<IrFunction>> =
            fileProto.originalToPreprocessedInlineFunctionsList.filterIndexed { index, _ -> index % 2 == 0 }
                .associate { originalDeclarationId ->
                    val originalIdSignature = symbolDeserializer.deserializeIdSignature(originalDeclarationId)
                    val preprocessedInlineFunctionId = originalSignatureToPreprocessed[originalDeclarationId]
                        ?: error("No preprocessed inline function found for $originalIdSignature")

                    val lazyDeclaration = lazy {
                        val declarationProto = fileReader.declaration(preprocessedInlineFunctionId)
                        declarationDeserializer.deserializeDeclaration(declarationProto) as IrFunction
                    }

                    originalIdSignature to lazyDeclaration
                }

        fun getTopLevelDeclarationOrNull(topLevelSignature: IdSignature): IrFunction? = indexWithLazyValues[topLevelSignature]?.value
    }
}

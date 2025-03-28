/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildTypealias
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.providers.utils.containingModule
import org.jetbrains.kotlin.sir.providers.utils.updateImport
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.relocatedDeclarationNamePrefix
import org.jetbrains.sir.lightclasses.utils.translatedAttributes

internal open class SirProtocolFromKtSymbol(
    override val ktSymbol: KaNamedClassSymbol,
    override val sirSession: SirSession,
) : SirProtocol(), SirFromKtSymbol<KaNamedClassSymbol> {
    override val origin: SirOrigin = KotlinSource(ktSymbol)
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val documentation: String? by lazy {
        ktSymbol.documentation()
    }
    override val name: String by lazy {
        (this.relocatedDeclarationNamePrefix() ?: "") + ktSymbol.name.asString()
    }
    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent(useSiteSession)
        }
        set(_) = Unit

    override val superClass: SirNominalType? by lazy {
        SirNominalType(KotlinRuntimeModule.kotlinBase)
    }

    override val protocols: List<SirProtocol> by lazyWithSessions {
        ktSymbol.superTypes
            .mapNotNull { it.symbol as? KaClassSymbol }
            .filter { it.classKind == KaClassKind.INTERFACE }
            .mapNotNull {
                it.toSir().allDeclarations.firstIsInstanceOrNull<SirProtocol>()?.also {
                    ktSymbol.containingModule.sirModule().updateImport(SirImport(it.containingModule().name))
                }
            }
    }

    override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }

    override val declarations: MutableList<SirDeclaration> by lazyWithSessions {
        ktSymbol.combinedDeclaredMemberScope
            .extractDeclarations(useSiteSession)
            .flatMap { declaration ->
                when (declaration) {
                    is SirVariable, is SirFunction -> listOf(declaration)
                    is SirNamedDeclaration -> listOf(
                        buildTypealias {
                            origin = SirOrigin.Trampoline(declaration)
                            visibility = SirVisibility.INTERNAL // visibility modifiers are disallowed in protocols
                            // FIXME: we make here the best effort to restore the original name of a relocated declaration
                            name = declaration.kaSymbolOrNull<KaDeclarationSymbol>()?.sirDeclarationName() ?: declaration.name
                            type = SirNominalType(declaration)
                        }.apply {
                            parent = this@SirProtocolFromKtSymbol
                        },
                        declaration
                    )
                    else -> listOf(declaration)
                }
            }
            .toMutableList()
    }
}

/**
 * A supporting extension declaration providing bridges for interface/protocol requirements for classes exported from kotlin.
 * Exporting a Kotlin class to Swift can result in overridden members from an inherited interface not aligning correctly with their
 * counterparts in the exported Swift protocol due to differences in Swift’s subtyping rules compared to Kotlin.
 * In such cases, Swift will use definitions from this extension to satisfy the missing protocol requirements.
 *
 * @property targetProtocol Protocol declaration this extension belongs to.
 */
internal class SirBridgedProtocolImplementationFromKtSymbol(
    override val ktSymbol: KaNamedClassSymbol,
    override val sirSession: SirSession,
    val targetProtocol: SirProtocol,
) : SirExtension(), SirFromKtSymbol<KaNamedClassSymbol> {
    constructor(protocol: SirProtocolFromKtSymbol) : this(protocol.ktSymbol, protocol.sirSession, protocol)

    override val origin: SirOrigin = KotlinSource(ktSymbol)

    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val documentation: String? by lazy {
        ktSymbol.documentation()
    }
    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.containingModule.sirModule()
        }
        set(_) = Unit

    override val extendedType: SirType
        get() = SirNominalType(targetProtocol)

    override val protocols: List<SirProtocol> get() = emptyList()

    override val constraints: List<SirTypeConstraint> by lazy {
        listOf(
            SirTypeConstraint.Conformance(SirNominalType(KotlinRuntimeSupportModule.kotlinBridged))
        )
    }

    override val attributes: List<SirAttribute> get() = emptyList()

    override val declarations: MutableList<SirDeclaration> by lazyWithSessions {
        ktSymbol.combinedDeclaredMemberScope
            .extractDeclarations(useSiteSession)
            .mapNotNull {
                when (it) {
                    is SirFunction -> SirRelocatedFunction(it).also { it.parent = this@SirBridgedProtocolImplementationFromKtSymbol }
                    is SirVariable -> SirRelocatedVariable(it).also { it.parent = this@SirBridgedProtocolImplementationFromKtSymbol }
                    else -> null
                }
            }
            .toMutableList()
    }
}

/**
 * Relocatied function
 * Mirrors the `source` declaration, but allows for chaning parent.
 *
 * @property source The original declaration
 */
private class SirRelocatedFunction(
    val source: SirFunction,
) : SirFunction() {
    override lateinit var parent: SirDeclarationParent

    override val origin: SirOrigin get() = source.origin
    override val visibility: SirVisibility get() = source.visibility
    override val documentation: String? get() = source.documentation
    override val name: String get() = source.name
    override val returnType: SirType get() = source.returnType
    override val isOverride: Boolean get() = false
    override val isInstance: Boolean get() = true
    override val modality: SirModality get() = SirModality.UNSPECIFIED
    override val attributes: List<SirAttribute> get() = source.attributes
    override val extensionReceiverParameter: SirParameter? get() = source.extensionReceiverParameter
    override val parameters: List<SirParameter> get() = source.parameters
    override val errorType: SirType get() = source.errorType

    override var body: SirFunctionBody?
        get() = source.body
        set(newValue) { source.body = newValue }
}

/**
 * Relocatied variable
 * Mirrors the `source` declaration, but allows for chaning parent.
 *
 * @property source The original declaration
 */
private class SirRelocatedVariable(
    val source: SirVariable,
) : SirVariable() {
    override lateinit var parent: SirDeclarationParent

    override val origin: SirOrigin get() = source.origin
    override val visibility: SirVisibility get() = source.visibility
    override val documentation: String? get() = source.documentation
    override val name: String get() = source.name
    override val type: SirType get() = source.type
    override val isOverride: Boolean get() = false
    override val isInstance: Boolean get() = true
    override val modality: SirModality get() = SirModality.UNSPECIFIED
    override val attributes: List<SirAttribute> get() = source.attributes
    override val getter: SirGetter get() = source.getter
    override val setter: SirSetter? get() = source.setter
}

internal class SirStubProtocol(
    ktSymbol: KaNamedClassSymbol,
    sirSession: SirSession
) : SirProtocolFromKtSymbol(
    ktSymbol,
    sirSession
) {
    override val declarations: MutableList<SirDeclaration> = mutableListOf()
}
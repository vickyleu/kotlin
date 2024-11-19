/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*

class K2JKlibCompilerArguments : CommonCompilerArguments() {
    companion object {
        @JvmStatic private val serialVersionUID = 0L
    }

    @Argument(value = "-d", valueDescription = "<directory|jar>", description = "Destination for generated .kotlin_metadata files.")
    var destination: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-classpath",
        shortName = "-cp",
        valueDescription = "<path>",
        description = "List of directories and JAR/ZIP archives to search for user .kotlin_metadata files."
    )
    var classpath: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-module-name", valueDescription = "<name>", description = "Name of the generated .kotlin_module file.")
    var moduleName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xfriend-paths",
        valueDescription = "<path>",
        description = "Paths to output directories for friend modules (modules whose internals should be visible)."
    )
    var friendPaths: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xklib",
        valueDescription = "<path>",
        description = "Paths to cross-platform libraries in the .klib format."
    )
    var klibLibraries: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-no-stdlib",
        description = "Don't automatically include the Kotlin/JVM stdlib and Kotlin reflection dependencies in the classpath."
    )
    var noStdlib = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-no-jdk", description = "Don't automatically include the Java runtime in the classpath.")
    var noJdk = false
        set(value) {
            checkFrozen()
            field = value
        }


    @Argument(value = "-no-reflect", description = "Don't automatically include the Kotlin reflection dependency in the classpath.")
    var noReflect = false
        set(value) {
            checkFrozen()
            field = value
        }
    @Argument(
        value = "-Xtype-enhancement-improvements-strict-mode",
        description = """Enable strict mode for improvements to type enhancement for loaded Java types based on nullability annotations,
including the ability to read type-use annotations from class files.
See KT-45671 for more details."""
    )
    var typeEnhancementImprovementsInStrictMode = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xenhance-type-parameter-types-to-def-not-null",
        description = "Enhance not-null-annotated type parameter types to definitely-non-nullable types ('@NotNull T' => 'T & Any')."
    )
    var enhanceTypeParameterTypesToDefNotNull = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xjvm-default",
        valueDescription = "{all|all-compatibility|disable}",
        description = """Emit JVM default methods for interface declarations with bodies. The default is 'disable'.
-Xjvm-default=all                Generate JVM default methods for all interface declarations with bodies in the module.
                                 Do not generate 'DefaultImpls' stubs for interface declarations with bodies. If an interface inherits a method with a
                                 body from an interface compiled in 'disable' mode and doesn't override it, then a 'DefaultImpls' stub will be
                                 generated for it.
                                 This BREAKS BINARY COMPATIBILITY if some client code relies on the presence of 'DefaultImpls' classes.
                                 Note that if interface delegation is used, all interface methods are delegated.
-Xjvm-default=all-compatibility  Like 'all', but additionally generate compatibility stubs in the 'DefaultImpls' classes.
                                 Compatibility stubs can help library and runtime authors maintain backward binary compatibility
                                 for existing clients compiled against previous library versions.
                                 'all' and 'all-compatibility' modes change the library ABI surface that will be used by clients after
                                 the recompilation of the library. Because of this, clients might be incompatible with previous library
                                 versions. This usually means that proper library versioning is required, for example with major version increases in SemVer.
                                 In subtypes of Kotlin interfaces compiled in 'all' or 'all-compatibility' mode, 'DefaultImpls'
                                 compatibility stubs will invoke the default method of the interface with standard JVM runtime resolution semantics.
                                 Perform additional compatibility checks for classes inheriting generic interfaces where in some cases an
                                 additional implicit method with specialized signatures was generated in 'disable' mode.
                                 Unlike in 'disable' mode, the compiler will report an error if such a method is not overridden explicitly
                                 and the class is not annotated with '@JvmDefaultWithoutCompatibility' (see KT-39603 for more details).
-Xjvm-default=disable            Default behavior. Do not generate JVM default methods."""
    )
    var jvmDefault: String = JvmDefaultMode.DISABLE.description
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xvalue-classes",
        description = "Enable experimental value classes."
    )
    var valueClasses = false
        set(value) {
            checkFrozen()
            field = value
        }


    override fun copyOf(): Freezable = TODO() // copyK2JKlibCompilerArguments(this, K2JKlibCompilerArguments())

    override fun configureAnalysisFlags(collector: MessageCollector, languageVersion: LanguageVersion): MutableMap<AnalysisFlag<*>, Any> =
        super.configureAnalysisFlags(collector, languageVersion).also {
            it[AnalysisFlags.metadataCompilation] = true
        }

    override fun configureLanguageFeatures(collector: MessageCollector): MutableMap<LanguageFeature, LanguageFeature.State> {
        val result = super.configureLanguageFeatures(collector)
        if (typeEnhancementImprovementsInStrictMode) {
            result[LanguageFeature.TypeEnhancementImprovementsInStrictMode] = LanguageFeature.State.ENABLED
        }
        if (enhanceTypeParameterTypesToDefNotNull) {
            result[LanguageFeature.ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated] = LanguageFeature.State.ENABLED
        }
        if (JvmDefaultMode.fromStringOrNull(jvmDefault)?.isEnabled == true) {
            result[LanguageFeature.ForbidSuperDelegationToAbstractFakeOverride] = LanguageFeature.State.ENABLED
            result[LanguageFeature.AbstractClassMemberNotImplementedWithIntermediateAbstractClass] = LanguageFeature.State.ENABLED
        }
        if (valueClasses) {
            result[LanguageFeature.ValueClasses] = LanguageFeature.State.ENABLED
        }
        return result
    }

    override fun configureExtraLanguageFeatures(map: HashMap<LanguageFeature, LanguageFeature.State>) {
        map[LanguageFeature.MultiPlatformProjects] = LanguageFeature.State.ENABLED
    }
}

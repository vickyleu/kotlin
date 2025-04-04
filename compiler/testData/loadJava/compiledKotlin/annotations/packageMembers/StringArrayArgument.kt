// PLATFORM_DEPENDANT_METADATA
// ALLOW_AST_ACCESS
// NO_CHECK_SOURCE_VS_BINARY
// IGNORE_BACKEND_K1: JVM_IR, ANY
// LANGUAGE: +MultiPlatformProjects
// K1/K2 difference: KT-60820

// IGNORE_FIR_METADATA_LOADING_K2_WITH_ANNOTATIONS_IN_METADATA
// ^ With annotations in metadata, compiler also loads `t = <implicitArrayOf>()` in some annotation classes.
// Once AnnotationsInMetadata is enabled by default, this directive can be removed and the txt dump can be updated.

// MODULE: common
// FILE: common.kt
package test

expect annotation class Anno4(vararg val t: String)
expect annotation class Anno5(vararg val t: String = [])
expect annotation class Anno6(vararg val t: String = ["a"])

// MODULE: platform()()(common)
// FILE: test.kt
package test

annotation class Anno(vararg val t: String)
annotation class Anno2(vararg val t: String = [])
annotation class Anno3(vararg val t: String = ["a"])
actual annotation class Anno4(actual vararg val t: String)
actual annotation class Anno5(actual vararg val t: String)
actual annotation class Anno6(actual vararg val t: String)

@Anno("live", "long") fun foo() {}

@field:Anno("prosper") val bar = { 42 }()

@Anno() @Anno2() @Anno3() @Anno4() @Anno5() @Anno6() fun baz() {}

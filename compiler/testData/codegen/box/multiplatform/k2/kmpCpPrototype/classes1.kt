// FIR_IDENTICAL
// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM

// MODULE: lib-common
// FILE: libCommon.kt
class Bar()

fun foo(a: String = "") = "OK"

// MODULE: lib-jvm()()(lib-common)
// FILE: libPlatform.kt

// MODULE: app-common(lib-common)()()
// FILE: appCommon.kt
expect fun bbb(): Bar

fun commonBox(): Bar = Bar()

// MODULE: app-jvm(lib-jvm)()(app-common)
// FILE: app.kt
actual fun bbb(): Bar = commonBox()

fun box(): String {
    commonBox()
    return "OK"
}

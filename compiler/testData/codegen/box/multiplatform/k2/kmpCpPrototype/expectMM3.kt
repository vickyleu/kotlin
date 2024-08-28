// FIR_IDENTICAL
// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM

// MODULE: lib-common
class Bar()

fun foo(a: String = "") = "common" // 1 klib

// MODULE: lib-jvm()()(lib-common)
fun foo() = "platform"

// MODULE: app-common(lib-common)()()
fun commonBox(): String = foo() // 1

// MODULE: app-jvm(lib-jvm)()(app-common)
fun box(): String {
    val commonBox = commonBox()
    val commonDirect = foo("")
    val platformDirect = foo()
    return if (commonBox == "common" && commonDirect == "common" && platformDirect == "platform") "OK"
    else "$commonBox/$commonDirect/$platformDirect"
}


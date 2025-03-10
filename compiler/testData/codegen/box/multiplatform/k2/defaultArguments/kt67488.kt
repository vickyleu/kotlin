// LANGUAGE: +MultiPlatformProjects

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization

// MODULE: common
// FILE: common.kt
expect annotation class A()

fun foo(@A x: Int) = "OK"

fun bar() = A()

// MODULE: platform()()(common)
// FILE: platform.kt
actual annotation class A(val value: String = "OK")

fun box(): String {
    foo(42).let {
        if (it != "OK") return it
    }

    bar().value.let {
        if (it != "OK") return it
    }

    return "OK"
}

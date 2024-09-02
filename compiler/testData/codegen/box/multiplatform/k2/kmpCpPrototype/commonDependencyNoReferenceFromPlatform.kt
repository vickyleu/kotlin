// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM

// MODULE: lib-common
class Foo {
    fun foo(): String = "O"
}

fun bar() = "K"

// MODULE: lib-jvm()()(lib-common)

// MODULE: app-common(lib-common)()()
fun commonBox(): String {
    val x = Foo()
    val o = x.foo()
    val k = bar()
    return "$o$k"
}

// MODULE: app-jvm(lib-jvm)()(app-common)
fun box(): String {
    return commonBox()
}

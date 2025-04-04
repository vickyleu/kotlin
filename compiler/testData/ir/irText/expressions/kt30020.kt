// LANGUAGE: -ForbidParenthesizedLhsInAssignments
// WITH_STDLIB
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6
// ^ MutableList has js specific methods

interface X {
    val xs: MutableList<Any>
    fun f(): MutableList<Any>
}

fun test(x: X, nx: X?) {
    x.xs += 1
    x.f() += 2
    (x.xs as MutableList<Int>) += 3
    (x.f() as MutableList<Int>) += 4
    nx?.xs!! += 5
    nx?.f()!! += 6
}

fun MutableList<Any>.testExtensionReceiver() {
    this += 100
}

abstract class AML : MutableList<Int> {
    fun testExplicitThis() {
        this += 200
    }

    inner class Inner {
        fun testOuterThis() {
            this@AML += 300
        }
    }
}

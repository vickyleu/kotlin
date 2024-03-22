// ISSUE: KT-66717

abstract class C<T> {
    protected abstract var x: T

    fun foo(y: T) {
        x = y
    }
}

interface I<T> {
    val x: T
        get() = TODO()
}

<!VAR_OVERRIDDEN_BY_VAL!>abstract class D : C<String>(), I<String> {
}<!>

fun main() {
    object : D() {
        override val x: String = "42"
    }.foo("1")
}

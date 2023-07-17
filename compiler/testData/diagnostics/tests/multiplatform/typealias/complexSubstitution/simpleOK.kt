// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect class E01<T> {
    fun takeT(t: T): String
    fun takeInt(i: Int): String
}

expect class E02<T, R> {
    fun takeT(t: T): String
    fun takeInt(i: Int): String
}

interface Base<T> {
    fun takeT(t: T): String
}

expect class E03<T> : Base<T> {
    fun takeInt(i: Int): String
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias E01<T> = A01<T, Int>

actual typealias E02<T, R> = A01<T, Int>

actual typealias E03<T> = A01<T, Int>

class A01<T, R> : Base<T> {
    override fun takeT(t: T): String {}
    fun takeInt(i: R): String {}
}

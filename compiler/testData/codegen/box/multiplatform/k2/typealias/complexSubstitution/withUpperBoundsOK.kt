// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect class E01<T> {
    fun takeT(t: T)
    fun takeInt(i: Int)
}

expect class E02<T : Number> {
    fun takeT(t: T)
    fun takeInt(i: Int)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias E01<T> = A01<T, Int>

actual typealias E02<T> = A02<T, Int>

class A01<T, R : Number> {
    fun takeT(t: T) {}
    fun takeInt(i: R) {}
}

class A02<T : Number, R> {
    fun takeT(t: T) {}
    fun takeInt(i: R) {}
}

// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect class E01<T> {
    fun takeT(t: T)
}

expect class E02<T : Number> {
    fun takeT(t: T)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias E01<T> = A01<T, TakeNumber<Int>>

actual typealias E02<T> = A02<T, TakeNumber<T>>

class A01<T, R> {
    fun takeT(t: T) {}
    fun takeR(i: R) {}
}

class A02<T : Number, R> {
    fun takeT(t: T) {}
    fun takeR(i: R) {}
}

class TakeNumber<T : Number>

// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect class E01<!NO_ACTUAL_FOR_EXPECT{JVM}!><T><!> {
    fun takeT(t: T)
}

expect class E02<!NO_ACTUAL_FOR_EXPECT{JVM}!><T : Number><!> {
    fun takeT(t: T)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias E01<!ACTUAL_WITHOUT_EXPECT!><T><!> = A01<T, TakeNumber<T>>

actual typealias E02<!ACTUAL_WITHOUT_EXPECT!><T><!> = A02<T, TakeNumber<T>>

class A01<T, R> {
    fun takeT(t: T) {}
    fun takeR(i: R) {}
}

class A02<T : CharSequence, R> {
    fun takeT(t: T) {}
    fun takeR(i: R) {}
}

class TakeNumber<T : Number>

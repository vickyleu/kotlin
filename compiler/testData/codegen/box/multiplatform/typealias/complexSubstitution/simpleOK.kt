// !LANGUAGE: +MultiPlatformProjects
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

fun commonBox(e01: E01<String>, e02: E02<String, Nothing>, e03: E03<String>): String {
    val probe = "42"

    if (e01.takeT(probe) != probe) return "c01"
    if (e02.takeT(probe) != probe) return "c02"
    if (e03.takeT(probe) != probe) return "c03"

    val num = 42
    if (e01.takeInt(num) != "$num") return "c04"
    if (e02.takeInt(num) != "$num") return "c05"
    if (e03.takeInt(num) != "$num") return "c06"

    return "O"
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias E01<T> = A01<T, Int>

actual typealias E02<T, R> = A01<T, Int>

actual typealias E03<T> = A01<T, Int>

class A01<T, R> : Base<T> {
    override fun takeT(t: T): String = t.toString()
    fun takeInt(i: R): String = i.toString()
}

fun platformBox(e01: E01<String>, e02: E02<String, Nothing>, e03: E03<String>): String {
    val probe = "42"

    if (e01.takeT(probe) != probe) return "p01"
    if (e02.takeT(probe) != probe) return "p02"
    if (e03.takeT(probe) != probe) return "p03"

    val num = 42
    if (e01.takeInt(num) != "$num") return "p04"
    if (e02.takeInt(num) != "$num") return "p05"
    if (e03.takeInt(num) != "$num") return "p06"

    return "K"
}

fun box(): String {
    val O = commonBox(E01<String>(), E02<String, Nothing>(), E03<String>())
    val K = platformBox(E01<String>(), E02<String, Nothing>(), E03<String>())

    return O + K
}

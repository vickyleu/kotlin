// WITH_STDLIB
// IGNORE_BACKEND: JVM
interface I<T> {
    fun foo(x: T): T
}

class C : I<Result<Any?>> {
    override fun foo(x: Result<Any?>) = x
}

fun box1() = C().foo(Result.success("OK")).getOrNull()

fun box() = box1() ?: "null"

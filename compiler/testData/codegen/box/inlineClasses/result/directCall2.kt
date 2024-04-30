// WITH_STDLIB
// IGNORE_BACKEND: JVM
interface I<T> {
    fun foo(x: T): Any?
}

class C : I<Result<Any?>> {
    override fun foo(x: Result<Any?>) = x.getOrNullNoinline()
}

fun <T> Result<T>.getOrNullNoinline() = getOrNull()

fun box1() = C().foo(Result.success("OK"))

fun box() = box1() ?: "null"

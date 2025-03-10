// WITH_STDLIB
// WITH_COROUTINES

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
import kotlin.coroutines.*

public inline fun <reified T> myEmptyArray(): Array<T> = arrayOfNulls<T>(0) as Array<T>

inline fun <reified T> Array<out T>?.myOrEmpty(): Array<out T> = this ?: myEmptyArray<T>()

fun <T> runBlocking(c: suspend () -> T): T {
    var res: T? = null
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        res = it.getOrThrow()
    })
    return res!!
}

suspend fun suspendHere(x: String) {}

suspend fun main() {
    arrayOf("1").myOrEmpty().forEach { suspendHere(it) }
}

fun box(): String {
    runBlocking(::main)
    return "OK"
}
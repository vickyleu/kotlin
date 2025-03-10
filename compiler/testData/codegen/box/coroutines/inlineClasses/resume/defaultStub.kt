// WITH_STDLIB

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

inline class IC(val s: String)

suspend fun foo(x: String = "OK") = suspendCoroutineUninterceptedOrReturn<IC> {
    it.resume(IC(x))
    COROUTINE_SUSPENDED
}

fun box(): String {
    var res = ""
    suspend { res = foo().s }.startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
    return res
}

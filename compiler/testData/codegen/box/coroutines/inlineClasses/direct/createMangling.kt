// WITH_STDLIB

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

inline class IC(val s: String)

fun box(): String {
    var res = "FAIL"
    val lambda: suspend (IC, IC) -> String = { a, b ->
        a.s + b.s
    }
    builder {
        res = lambda(IC("O"), IC("K"))
    }
    return res
}
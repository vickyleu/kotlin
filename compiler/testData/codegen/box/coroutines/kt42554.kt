// WITH_STDLIB

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
import kotlin.coroutines.*

fun launch(block: suspend () -> String): String {
    var result = ""
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it.getOrThrow() })
    return result
}

enum class E { A }

class C(val e: E) {
    val result = launch {
        when (e) {
            E.A -> "OK"
        }
    }
}

fun box(): String = C(E.A).result

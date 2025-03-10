// WITH_STDLIB

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
import kotlin.coroutines.*

fun box(): String = a { (::write).let { it() } }

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun a(a: suspend Writer.() -> String): String {
    var res = ""
    builder { res = Writer().a() }
    return res
}

class Writer {
    fun write(): String = "OK"
}
// WITH_STDLIB

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
import kotlin.coroutines.*

fun runs(f: suspend () -> String): String {
    var result: String? = null
    f.startCoroutine(
        Continuation(EmptyCoroutineContext) {
            result = it.getOrThrow()
        }
    )
    return result ?: "Fail"
}

suspend fun suspendListOf(s: String) = listOf(s)

val strings: MutableCollection<String> = ArrayList()

fun box(): String {
    return runs {
        strings += suspendListOf("OK")
        strings.iterator().next()
    }
}

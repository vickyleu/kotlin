// WITH_STDLIB

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
import kotlin.coroutines.*

fun launch(block: suspend () -> String): String {
    var result = ""
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it.getOrThrow() })
    return result
}


private class CharTest {
    private val test: Char = '!'

    fun simpleTest() = launch {
        val ch = get()
        if (ch == '!') "OK" else "Fail"
    }

    suspend fun get(): Char? = test
}


fun box(): String = CharTest().simpleTest()

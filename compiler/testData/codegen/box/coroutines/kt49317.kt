// WITH_STDLIB

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization

// FILE: 2.kt
package other
import builders.*
import kotlin.coroutines.*

fun test() {
    suspend {
        foo {  }
    }.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    test()
    return "OK"
}

// FILE: 1.kt
package builders

suspend fun foo(
    a: suspend () -> Unit = {}
) {}
// WITH_STDLIB
// TARGET_BACKEND: WASM
// WASM_FAILS_IN: JSC // Related issue https://bugs.webkit.org/show_bug.cgi?id=277547
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

import kotlin.test.*

fun box(): String {
    val x = try {
        throw Error()
        5
    } catch (e: Throwable) {
        6
    }

    assertEquals(6, x)
    return "OK"
}
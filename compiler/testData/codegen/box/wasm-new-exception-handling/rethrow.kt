// WITH_STDLIB
// TARGET_BACKEND: WASM
// WASM_FAILS_IN: JSC // Related issue https://bugs.webkit.org/show_bug.cgi?id=277547
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

import kotlin.test.*

fun box(): String {
    assertFailsWith<IllegalStateException>("My error") {
        try {
            error("My error")
        } catch (e: Throwable) {
            throw e
        }
    }
    return "OK"
}

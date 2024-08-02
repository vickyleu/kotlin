// WITH_STDLIB
// TARGET_BACKEND: WASM
// WASM_FAILS_IN: JSC // Related issue https://bugs.webkit.org/show_bug.cgi?id=277547
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    try {
        foo()
    } catch (e: Throwable) {
        val message = e.message
        if (message != null) {
            sb.append(message)
        }
    }

    return sb.toString()
}

fun foo() {
    throw Error("OK")
}
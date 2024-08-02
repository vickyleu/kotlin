// WITH_STDLIB
// TARGET_BACKEND: WASM
// WASM_FAILS_IN: JSC // Related issue https://bugs.webkit.org/show_bug.cgi?id=277547
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    try {
        sb.appendLine("Before")
        foo()
        sb.appendLine("After")
    } catch (e: Throwable) {
        sb.appendLine("Caught Throwable")
    }

    sb.appendLine("Done")

    assertEquals("""
        Before
        Caught Throwable
        Done

    """.trimIndent(), sb.toString())
    return "OK"
}

fun foo() {
    throw Error("Error happens")
    sb.appendLine("After in foo()")
}
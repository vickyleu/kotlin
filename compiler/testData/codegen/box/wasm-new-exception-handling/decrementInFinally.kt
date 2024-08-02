// TARGET_BACKEND: WASM
// WASM_FAILS_IN: JSC // Related issue https://bugs.webkit.org/show_bug.cgi?id=277547
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved
fun box(): String {
    var variable = 0
    try {
        try {
            null!!
        } finally {
            variable--
        }
    } catch (e: NullPointerException) {
        return if (variable == -1) "OK" else "Fail"
    }
}
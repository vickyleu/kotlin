// TARGET_BACKEND: WASM
// WASM_FAILS_IN: JSC
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
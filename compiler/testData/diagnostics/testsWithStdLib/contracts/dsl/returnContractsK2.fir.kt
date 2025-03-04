// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DUMP_CFG

import kotlin.contracts.*

fun decode(encoded: String?): String? {
    contract {
        (encoded != null) implies (returnsNotNull())
    }
    if (encoded == null) return null
    return encoded + "a"
}

fun test() {
    // smartcast
    val x = "hello"
    decode(x).length
}


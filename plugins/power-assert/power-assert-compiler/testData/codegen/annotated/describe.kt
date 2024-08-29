// DUMP_KT_IR

import kotlin.explain.*

fun box(): String {
    val reallyLongList = listOf("a", "b")
    return describe(reallyLongList.reversed() == emptyList<String>()) ?: "FAIL"
}

@ExplainCall
fun describe(value: Any): String? {
    return ExplainCall.explanation?.toDefaultMessage()
}

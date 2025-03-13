// LANGUAGE: -IrInlinerBeforeKlibSerialization
// ^^^ KT-75975
// WITH_STDLIB

fun f1(): () -> Result<String> {
    return {
        runCatching {
            "OK"
        }
    }
}

fun box(): String {
    val r = f1()()
    return r.getOrNull() ?: "fail: $r"
}
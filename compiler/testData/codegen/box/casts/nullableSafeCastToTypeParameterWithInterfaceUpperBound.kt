// WASM_MUTE_REASON: NULLABLE_BOX_FUNCTION
interface I

fun <E: I> foo(a: Any?): E? = a as? E

fun box1() = foo<I>(null) ?: "OK"

fun box() = box1() ?: "null"

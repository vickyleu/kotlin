// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
fun <T> localWithTypeParam(b: T): T {
    context(a: T)
    fun foo(): T {
        return a
    }

    with(b) {
        return foo()
    }
}

fun box(): String {
    return localWithTypeParam("OK")
}
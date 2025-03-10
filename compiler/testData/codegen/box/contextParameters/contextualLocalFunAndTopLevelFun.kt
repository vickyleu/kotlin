// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
fun context(a: String) { }

fun box(): String {
    context(a: String)
    fun foo(): String {
        return a
    }
    with("OK") {
        return foo()
    }
}
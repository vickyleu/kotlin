// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
context(a: String)
val p
    get() = context(a: String) fun (): String { return a }

fun box(): String {
    with("not OK") {
        return p("OK")
    }
}
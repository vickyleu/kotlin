// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
fun box(): String {
    with("O") {
        return (context(a: String) fun (y: String): String = a + y)("K")
    }
}
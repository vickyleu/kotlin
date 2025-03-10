// WITH_STDLIB

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
fun box(): String {
    listOf(1).forEach { size ->
        repeat(size) {
            return "OK"
        }
    }
    return "Fail"
}

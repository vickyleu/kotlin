// WITH_STDLIB

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization

fun foo() {
    with(1) {
        return (1..2).forEach { it }
    }
}

fun box(): String {
    foo()
    return "OK"
}

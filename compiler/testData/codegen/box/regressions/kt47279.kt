// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
fun test() {
    null?.run { return }
}

fun box(): String {
    test()
    return "OK"
}

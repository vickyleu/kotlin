// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
class C(var a: String) {
    fun foo(): String { return a }
}

context(a: C)
fun test(b: C = a): String {
    return b.foo()
}

fun box(): String {
    with(C("OK")) {
        return test()
    }
}
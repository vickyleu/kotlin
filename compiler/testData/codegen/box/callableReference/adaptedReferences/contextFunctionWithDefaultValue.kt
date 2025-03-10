// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
class C(var a: String) {
    fun foo(): String { return a }
}

fun funWithContextAndValueType(x: context(C) () -> String): String {
    with(C("OK")) {
        return x()
    }
}

fun valueParamFunWithDefault(c: C = C("NOT OK")): String { return c.foo() }

fun box(): String {
    return funWithContextAndValueType(::valueParamFunWithDefault)
}
// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
class Test<T> {
    fun T.foo(): String {
        return this as String
    }
}

fun box(): String {
    with(Test<String>()) {
        return "OK".foo()
    }
}
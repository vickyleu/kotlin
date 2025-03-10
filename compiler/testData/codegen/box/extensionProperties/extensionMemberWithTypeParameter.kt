// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
class Test<T> {
    val T.foo: String
        get() = this as String
}

fun box(): String {
    with(Test<String>()){
        return "OK".foo
    }
}
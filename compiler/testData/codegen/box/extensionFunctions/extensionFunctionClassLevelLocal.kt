// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
fun foo(): String {
    class Local<T> {
        fun Local<String>.bar(): String {
            return "OK"
        }
    }
    with(Local<String>()){
        return Local<String>().bar()
    }
}

fun box(): String = foo()
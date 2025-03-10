// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
fun interface SamInterface {
    fun Int.accept(i: String): String
}

val a = SamInterface { a: String ->  "OK" }

fun box(): String {
    with(a) {
        return 1.accept("")
    }
}
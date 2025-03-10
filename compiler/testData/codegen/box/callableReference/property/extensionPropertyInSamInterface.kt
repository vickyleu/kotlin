// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
fun interface SamInterface {
    fun Int.accept(): String
}

val Int.a : String
    get() = "OK"

val b = SamInterface (Int::a)

fun box(): String {
    with(b) {
        return 1.accept()
    }
}
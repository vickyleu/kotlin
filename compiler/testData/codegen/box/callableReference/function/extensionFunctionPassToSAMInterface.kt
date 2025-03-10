// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
fun interface SamInterface {
    fun accept(a: Int, i: String): String

}
fun Int.foo(i: String): String = i
val a = SamInterface (Int::foo)

fun box(): String {
    with(a) {
        return accept(1, "OK")
    }
}
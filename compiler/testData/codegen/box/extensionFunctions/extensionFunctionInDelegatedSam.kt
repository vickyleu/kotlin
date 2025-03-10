// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
fun interface Base {
    fun String.print(): String
}

class Derived(b: Base) : Base by b

fun box(): String {
    val a = Derived(Base{ this })
    with(a){
        return "OK".print()
    }
}

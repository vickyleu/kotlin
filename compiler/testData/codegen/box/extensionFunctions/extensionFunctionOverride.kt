// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
interface Base {
    fun Int.foo(): String
    val Int.a : String
}

class A : Base {
    override fun Int.foo(): String {
        return "O"
    }

    override val Int.a: String
        get() = "K"
}

fun box(): String {
    with(A()){
        return 1.foo() + 1.a
    }
}
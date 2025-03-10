// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
class A(val a: Int) {
    fun A.foo(): Boolean {
        return this.a == this@A.a
    }
}

fun box(): String {
    with(A(1)) {
        return if (A(2).foo()) "fail"
        else "OK"
    }
}
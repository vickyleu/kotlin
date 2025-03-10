// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
class A {
    class A
    fun A.foo(): String {
        return "OK"
    }
}

fun box(): String {
    with(A()){
        return A.A().foo()
    }
}
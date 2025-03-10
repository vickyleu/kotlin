// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-73779
// LANGUAGE: +ContextParameters

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
class A

class Base {
    context(a: A)
    fun funMember(): String { return "OK" }
}

context(a: A)
fun Base.funMember(): String { return "not OK" }

fun box(): String {
    with(A()) {
        return Base().funMember()
    }
}
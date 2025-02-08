import kotlin.reflect.KClass

interface I {
    fun f()
}

open class A : I {
    override fun f() {
        println("!!!")
    }
}



open class B : A() {

}

class C : B() {

}



fun keklol1(a: I): KClass<*> {
    return a::class
}

fun keklol2(): KClass<*> {
    return A::class
}

fun lolkek() {
//    println(keklol1(A()) == keklol2())
//
//    println(A()::class.qualifiedName)
//
//
//    println(A()::class.qualifiedName)
//    println(A::class.qualifiedName)
//    println(A()::class == A::class)
//    println(A::class)
//    println(A()::class)

    println(A::class.isInstance(C()))
//    println(A()::class.simpleName)

}

fun lolkek1(a: I) {
    a.f()
}

fun box(): String {

    lolkek1(A())
    lolkek()


    return "OK"
}
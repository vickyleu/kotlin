// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
@Target(AnnotationTarget.PROPERTY)
annotation class Anno

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val s: String)

class A {
    @Anno
    val Z.r: String get() = s
}

fun box(): String {
    with(A()) {
        return Z("OK").r
    }
}

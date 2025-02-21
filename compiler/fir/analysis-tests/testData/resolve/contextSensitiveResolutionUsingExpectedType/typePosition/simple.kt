// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class A {
    X, Y
}

fun foo(a: A) {}

fun main() {
    foo(X)
}

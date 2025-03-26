// ISSUE: KT-76024
// NO_CHECK_LAMBDA_INLINING
// IGNORE_INLINER: IR
// ^^^ KT-76148: No mapping for symbol: VAR name:x type:kotlin.internal.Ref.ObjectRef<kotlin.String>

// FILE: 1.kt
package test

inline fun <A> test(
    x: () -> A,
    y: A.() -> String
): String = y(x())

// FILE: 2.kt
import test.*

fun box() = test(
    {
        var x = "FAIL"
        fun local() = x
        val result = object { inline fun foo() = local() }
        x = "OK"
        result
    },
    { foo() },
)

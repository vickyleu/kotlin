// ISSUE: KT-62918
// FIR_DUMP

class My<T>(val value: T)
interface I1
interface I2

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>fun foo()<!> = My(object : I1, I2 {})

fun fooFoo(): My<I1> = My(object : I1, I2 {})

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal fun bar()<!> = My(object : I1, I2 {})

private fun baz() = My(object : I1, I2 {})

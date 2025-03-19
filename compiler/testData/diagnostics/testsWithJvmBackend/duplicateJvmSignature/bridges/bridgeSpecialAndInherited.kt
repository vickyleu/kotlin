// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-13712

open class SizeProvider {
    open fun size() = 0
}

<!CONFLICTING_INHERITED_JVM_DECLARATIONS!>class ListImpl(private val lst: List<Int>) : List<Int> by lst, SizeProvider()<!>
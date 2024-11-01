// ISSUE: KT-72335
// FIR_DUMP
// DUMP_CFG

fun foo(b: Boolean, block: (Int.() -> Unit)) {
    block(1.<!ILLEGAL_SELECTOR!>{ if (b) "s1" else "s3" }<!>)
}

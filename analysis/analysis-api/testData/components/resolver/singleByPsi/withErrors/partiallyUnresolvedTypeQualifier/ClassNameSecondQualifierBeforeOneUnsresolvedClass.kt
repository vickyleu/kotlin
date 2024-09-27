
class Foo {
    class Bar
}

class C {
    val prop: Foo.<caret>Bar.Baz
}

fun <T> take(action: (T) -> Unit) {}

val prop = take {
    prop: Foo.<caret_lambda>Bar.Baz ->
}



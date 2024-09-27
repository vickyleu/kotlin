
class Foo {
    class Bar
}

class C {
    val prop: <caret>Foo.Bar.Baz.Bazzzz
}

fun <T> take(action: (T) -> Unit) {}

val prop = take {
    prop: <caret_lambda>Foo.Bar.Bazzzz ->
}


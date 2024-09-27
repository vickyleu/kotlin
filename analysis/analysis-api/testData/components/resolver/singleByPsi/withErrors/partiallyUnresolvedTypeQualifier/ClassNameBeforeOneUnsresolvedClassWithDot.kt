
class Foo

class C {
    val prop: <caret>Foo.Bar.
}

fun <T> take(action: (T) -> Unit) {}

val prop = take {
    prop: <caret_lambda>Foo.Bar. ->
}



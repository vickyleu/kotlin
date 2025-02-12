// ISSUE: KT-75061

sealed interface MySealed {
    class  Left<out E>(val x: String): MySealed
    class Right<out A>(val y: String): MySealed
}

fun <E, A> Either<E, A>.getOrElse(default: A) = when (this) {
    is Left /* Should work */  -> default
    is Right /* Should work */ -> value
}

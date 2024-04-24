// FIR_IDENTICAL
// TARGET_FRONTEND: FIR
// ISSUE: KT-64515

open class Data<A>(val initial: A)

class DataSub<A>(initial: A) : Data<A>(initial)

class Widget<B : DataSub<C>, C>(val data: B)

class WidgetWrapper<D : Data<E>, E>(val data: D)

fun foo(w: Widget<*, *>) {
    WidgetWrapper(data = w.data)
}

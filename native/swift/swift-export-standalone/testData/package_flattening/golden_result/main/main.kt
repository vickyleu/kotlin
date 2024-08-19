import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@kotlinx.cinterop.internal.CCall("SwiftExport_ExportedKotlinPackages_org_kotlin_foo_Clazz_toRetainedSwift")
@kotlin.native.internal.ref.ToRetainedSwift(org.kotlin.foo.Clazz::class)
external fun SwiftExport_ExportedKotlinPackages_org_kotlin_foo_Clazz_toRetainedSwift(ref: kotlin.native.internal.ref.ExternalRCRef): kotlin.native.internal.NativePtr

@ExportedBridge("org_kotlin_foo_Clazz_init_allocate")
public fun org_kotlin_foo_Clazz_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<org.kotlin.foo.Clazz>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_Clazz_init_initialize__TypesOfArguments__uintptr_t__")
public fun org_kotlin_foo_Clazz_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, org.kotlin.foo.Clazz())
}

@ExportedBridge("org_kotlin_foo_constant_get")
public fun org_kotlin_foo_constant_get(): Int {
    val _result = org.kotlin.foo.constant
    return _result
}

@ExportedBridge("org_kotlin_foo_function__TypesOfArguments__int32_t__")
public fun org_kotlin_foo_function(arg: Int): Int {
    val __arg = arg
    val _result = org.kotlin.foo.function(__arg)
    return _result
}

@ExportedBridge("org_kotlin_foo_variable_get")
public fun org_kotlin_foo_variable_get(): Int {
    val _result = org.kotlin.foo.variable
    return _result
}

@ExportedBridge("org_kotlin_foo_variable_set__TypesOfArguments__int32_t__")
public fun org_kotlin_foo_variable_set(newValue: Int): Unit {
    val __newValue = newValue
    org.kotlin.foo.variable = __newValue
}


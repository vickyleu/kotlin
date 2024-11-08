// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// CHECK-LABEL: define i32 @"kfun:#test1(kotlin.Any){}kotlin.Int
fun test1(o: Any): Int {
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK: call i32 @"kfun:kotlin.String#<get-length>(){}kotlin.Int
    return if (o is String) o.length else 42
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test2(kotlin.Any){}kotlin.Int
fun test2(o: Any): Int {
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK: call i32 @"kfun:kotlin.String#<get-length>(){}kotlin.Int
    return (o as? String)?.length ?: 42
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test3(kotlin.Any){}kotlin.Int
fun test3(o: Any): Int {
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    return if (o as? String != null)
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK: call i32 @"kfun:kotlin.String#<get-length>(){}kotlin.Int
        o.length
    else 42
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test4(kotlin.Any){}kotlin.Int
fun test4(o: Any): Int {
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    val temp = when (o) {
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK: call i32 @"kfun:kotlin.String#<get-length>(){}kotlin.Int
        is String -> o.length
        else -> 42
    }
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK: {{call|call zeroext}} i16 @Kotlin_String_get
    return temp + ((o as? String)?.get(0)?.code ?: 0)
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test5(kotlin.Int;kotlin.Any){}kotlin.Int
fun test5(x: Int, o: Any): Int {
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    val result = if (x == 42 || o is String)
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK: call i32 @"kfun:kotlin.String#<get-length>(){}kotlin.Int
        (o as? String)?.length ?: 0
    else
        x
    return result
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define {{i1|zeroext i1}} @"kfun:#baz(kotlin.String){}kotlin.Boolean"
fun baz(s: String) = s.length == 3
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define i32 @"kfun:#test6(kotlin.Int;kotlin.Any){}kotlin.Int
fun test6(x: Int, o: Any): Int {
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    val result = if (x == 42 || baz(o as String))
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK: call i32 @"kfun:kotlin.String#<get-length>(){}kotlin.Int
        (o as? String)?.length ?: 0
    else
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK: {{call|call zeroext}} i16 @Kotlin_String_get
        (o as? String)?.get(0)?.code ?: x
    return result
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test7(kotlin.Int;kotlin.Any){}kotlin.Int
fun test7(x: Int, o: Any): Int {
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    if (x == 42 || baz(o as String))
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK: call i32 @"kfun:kotlin.String#<get-length>(){}kotlin.Int
        return (o as? String)?.length ?: 0
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK: {{call|call zeroext}} i16 @Kotlin_String_get
    return (o as? String)?.get(0)?.code ?: x
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    println(test1("zzz"))
    println(test2("zzz"))
    println(test3("zzz"))
    println(test4("zzz"))
    println(test5(1, "zzz"))
    println(test6(1, "zzz"))
    println(test7(1, "zzz"))
    return "OK"
}

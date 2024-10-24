// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// CHECK-LABEL: define i32 @"kfun:#test1(kotlin.Any){}kotlin.Int
fun test1(o: Any): Int {
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK: call i32 @"kfun:kotlin.String#<get-length>(){}kotlin.Int
    return (o as? String)?.length ?: 42
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test2(kotlin.Any){}kotlin.Int
fun test2(o: Any): Int {
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

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    println(test1("zzz"))
    println(test2("zzz"))
    return "OK"
}

// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

class A(val s: String, val x: Int, val y: Int)

// CHECK-LABEL: define i32 @"kfun:#test1(kotlin.Any){}kotlin.Int
fun test1(o: Any): Int {
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    return if (o is A)
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        o.x
    else 42
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test2(kotlin.Any){}kotlin.Int
fun test2(o: Any): Int {
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
    return (o as? A)?.x ?: 42
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test3(kotlin.Any){}kotlin.Int
fun test3(o: Any): Int {
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    return if (o as? A != null)
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        o.x
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
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        is A -> o.x
        else -> 42
    }
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-y>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
    return temp + ((o as? A)?.y ?: 0)
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test5(kotlin.Int;kotlin.Any){}kotlin.Int
fun test5(x: Int, o: Any): Int {
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    val result = if (x == 42 || o is A)
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        (o as? A)?.x ?: 0
    else
        x
    return result
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define {{i1|zeroext i1}} @"kfun:#baz(A){}kotlin.Boolean"
fun baz(a: A) = a.x == 3
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define i32 @"kfun:#test6(kotlin.Int;kotlin.Any){}kotlin.Int
fun test6(x: Int, o: Any): Int {
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    val result = if (x == 42 || baz(o as A))
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        (o as? A)?.x ?: 0
    else
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-y>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        (o as? A)?.y ?: x
    return result
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test7(kotlin.Int;kotlin.Any){}kotlin.Int
fun test7(x: Int, o: Any): Int {
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    if (x == 42 || baz(o as A))
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        return (o as? A)?.x ?: 0
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-y>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
    return (o as? A)?.y ?: x
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test8(kotlin.Any){}kotlin.Int
fun test8(o: Any): Int {
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    return if ((o as? A)?.s?.length == 5)
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        o.x
    else 42
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test9(A?){}kotlin.Int
fun test9(s: A?): Int {
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
    return if (s?.x == 5)
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-y>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        s.y
    else 42
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test10(kotlin.Int;kotlin.Any){}kotlin.Int
fun test10(x: Int, o: Any): Int {
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    val a = o as? A
    val y = x + x
    return if (a != null)
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        o.x
    else y
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test11(kotlin.Int;kotlin.Any){}kotlin.Int
fun test11(x: Int, o: Any): Int {
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    val f = o is A
    val y = x + x
    return if (f)
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        o.x
    else y
// CHECK-LABEL: epilogue:
}

fun getAny(): Any = A("zzz", 42, 117)

// CHECK-LABEL: define i32 @"kfun:#test12(kotlin.Int;kotlin.Any){}kotlin.Int
fun test12(x: Int, o: Any): Int {
    var mutO = o
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    val a = mutO as? A
    val y = x + x
    val z = if (a != null)
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        (mutO as A).x
    else y

// CHECK: call ptr @"kfun:#getAny(){}kotlin.Any
    mutO = getAny()
    return if (a != null)
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-y>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        (mutO as? A)?.y ?: y
    else z
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test13(kotlin.Int;kotlin.Any){}kotlin.Int
fun test13(x: Int, o: Any): Int {
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    var a = o as? A
    val y = x + x
    val z = if (a != null)
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        (o as A).x
    else y
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    a = getAny() as? A
    return if (a != null)
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-y>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        (o as? A)?.y ?: y
    else z
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test14(kotlin.Int;kotlin.Any){}kotlin.Int
fun test14(x: Int, o: Any): Int {
    var mutO = o
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    val f = mutO is A
    val y = x + x
    val z = if (f)
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        (mutO as A).x
    else y

// CHECK: call ptr @"kfun:#getAny(){}kotlin.Any
    mutO = getAny()
    return if (f)
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-y>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        (mutO as? A)?.y ?: y
    else z
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test15(kotlin.Int;kotlin.Any){}kotlin.Int
fun test15(x: Int, o: Any): Int {
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    var f = o is A
    val y = x + x
    val z = if (f)
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        (o as A).x
    else y
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    f = getAny() is A
    return if (f)
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-y>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        (o as? A)?.y ?: y
    else z
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test16(kotlin.Int;kotlin.Any;kotlin.Any){}kotlin.Int
fun test16(x: Int, a: Any, b: Any): Int {
    val o: Any
    if (x > 0)
        o = a
    else
        o = b
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    if (o is A) {
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        return o.x +
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-y>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
                ((a as? A)?.y ?: x) +
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-y>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
                ((b as? A)?.y ?: x)
    }
    return 0
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test17(kotlin.Int;kotlin.Any){}kotlin.Int
fun test17(x: Int, a: Any): Int {
    val o: Any
    if (x > 0)
        o = a
    else return 0
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    return if (o is A) {
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        o.x +
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-y>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
                ((a as? A)?.y ?: x)
    } else -1
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test18(kotlin.Int;kotlin.Any){}kotlin.Int
fun test18(x: Int, a: Any): Int {
    val o: Any
    if (x > 0)
        o = a
    else return 0
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    return if (a is A) {
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        a.x +
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-y>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
                ((o as? A)?.y ?: x)
    } else -1
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test19(kotlin.Int;kotlin.Any;kotlin.Any){}kotlin.Int
fun test19(x: Int, a: Any, b: Any): Int {
    var o: Any = a
    if (x > 0)
        o = b
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    if (o is A) {
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        return o.x +
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-y>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
                ((a as? A)?.y ?: x) +
// TODO
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-y>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
                ((b as? A)?.y ?: x)
    }
    return 0
// CHECK-LABEL: epilogue:
}

inline fun <T> inlineFun(x: Int, block: (Boolean) -> T): T = block(x > 0)

// CHECK-LABEL: define i32 @"kfun:#test20(kotlin.Int;kotlin.Any;kotlin.Any){}kotlin.Int
fun test20(x: Int, a: Any, b: Any): Int {
    val o = inlineFun(x) {
        if (it)
            return@inlineFun a
        else
            return@inlineFun b
    }
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    if (o is A) {
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        return o.x +
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-y>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
                ((a as? A)?.y ?: x) +
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-y>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
                ((b as? A)?.y ?: x)
    }
    return 0
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test21(kotlin.Int;kotlin.Any){}kotlin.Int
fun test21(x: Int, a: Any): Int {
    val o = inlineFun(x) {
        if (it)
            return@inlineFun a
        else return 0
    }
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    return if (o is A) {
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        o.x +
// TODO
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-y>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
                ((a as? A)?.y ?: x)
    } else -1
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define i32 @"kfun:#test22(kotlin.Int;kotlin.Any){}kotlin.Int
fun test22(x: Int, a: Any): Int {
    val o = inlineFun(x) {
        if (it)
            return@inlineFun a
        else return 0
    }
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
    return if (a is A) {
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-x>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
        a.x +
// TODO
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:A#<get-y>(){}kotlin.Int
// CHECK-OPT: getelementptr inbounds %"kclassbody:A#internal
                ((o as? A)?.y ?: x)
    } else -1
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    val a = A("zzz", 42, 117)
    println(test1(a))
    println(test2(a))
    println(test3(a))
    println(test4(a))
    println(test5(1, a))
    println(test6(1, a))
    println(test7(1, a))
    println(test8(a))
    println(test9(a))
    println(test10(1, a))
    println(test11(1, a))
    println(test12(1, a))
    println(test13(1, a))
    println(test14(1, a))
    println(test15(1, a))
    println(test16(1, a, a))
    println(test17(1, a))
    println(test18(1, a))
    println(test19(1, a, a))
    println(test20(1, a, a))
    println(test21(1, a))
    println(test22(1, a))
    return "OK"
}

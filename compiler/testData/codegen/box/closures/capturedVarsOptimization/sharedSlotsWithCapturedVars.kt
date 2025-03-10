// WITH_STDLIB

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
fun box(): String {
    run {
        run {
            var x = 0
            run { ++x }
            if (x == 0) return "fail"
        }

        run {
            var x = 0
            run { x++ }
            if (x == 0) return "fail"
        }
    }

    return "OK"
}
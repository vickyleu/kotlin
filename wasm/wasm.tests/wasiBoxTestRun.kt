/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

//import foo.*

@kotlin.wasm.WasmExport
fun runBoxTest(): Boolean {
    var done = false
    try {
        val boxResult = box() //TODO: Support non-root package box functions
        done = true
        val isOk = boxResult == "OK"
        if (!isOk) {
            println("Wrong box result '${boxResult}'; Expected 'OK'")
        }
        return isOk
    } catch (e: Throwable) {
        println("Uncaught exception: $e")
        done = true
        fail()
        return false
    } finally {
        if (!done) {
            println("Something went wrong!")
            fail()
            return false
        }
    }

    return false
}
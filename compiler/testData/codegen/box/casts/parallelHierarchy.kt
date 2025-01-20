// XRUN_THIRD_PARTY_OPTIMIZER

interface I1
interface I2

class C1 : I1
class C2 : I2

fun checkI1(o: Any): Boolean = o is I1
fun checkI2(o: Any): Boolean = o is I2
fun checkC1(o: Any): Boolean = o is C1
fun checkC2(o: Any): Boolean = o is C2

interface SAMI1 {
    fun f(x: Int)
}
interface SAMI2 {
    fun g()
}
class CSAM : SAMI1 {
    override fun f(x: Int) {
        println()
    }

}

val x: (Int) -> Unit = {}

fun checkSAMI1(o: Any): Boolean = o is SAMI1
fun checkSAMI2(o: Any): Boolean {
    o as (Int) -> Unit
    return true
}



fun lolkek(o: SAMI1) {
    o.f(2)
}

@kotlin.wasm.WasmExport
fun keklol() {
    val csam = CSAM()
    lolkek(csam)
}

fun box(): String {
    val c1 = C1()
    val c2 = C2()
    val any = Any()

//    if (!checkI1(c1)) return "FAIL1"
//    if (checkI2(c1)) return "FAIL2"
//    if (!checkC1(c1)) return "FAIL3"
//    if (checkC2(c1)) return "FAIL4"
//
//    if (checkI1(c2)) return "FAIL5"
//    if (!checkI2(c2)) return "FAIL6"
//    if (checkC1(c2)) return "FAIL7"
//    if (!checkC2(c2)) return "FAIL8"
//
//    if (checkI1(any)) return "FAIL9"
//    if (checkI2(any)) return "FAIL10"
//    if (checkC1(any)) return "FAIL11"
//    if (checkC2(any)) return "FAIL12"


    keklol()
//    if (!checkSAMI1(csam)) return "FAIL13"
//    if (checkSAMI2(csam)) return "FAIL14"

    return "OK"
}
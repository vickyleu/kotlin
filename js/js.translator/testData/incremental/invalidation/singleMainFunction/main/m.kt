fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0 -> if (A.mainInA || !B.mainInB) return "FAIL1"
        1 -> if (!A.mainInA || B.mainInB) return "FAIL2"
        else -> return "Unknown"
    }
    return "OK"
}

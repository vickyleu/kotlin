// Auto-generated by org.jetbrains.kotlin.generators.tests.GenerateRangesCodegenTestData. DO NOT EDIT!
// WITH_STDLIB

fun box(): String {
    val list1 = ArrayList<UInt>()
    val range1 = (1u + 2u)..(6u - 1u)
    for (i in range1) {
        list1.add(i)
        if (list1.size > 23) break
    }
    if (list1 != listOf<UInt>(3u, 4u, 5u)) {
        return "Wrong elements for (1u + 2u)..(6u - 1u): $list1"
    }

    val list2 = ArrayList<UInt>()
    val range2 = (1u.toUByte() + 2u.toUByte()).toUByte()..(6u.toUByte() - 1u.toUByte()).toUByte()
    for (i in range2) {
        list2.add(i)
        if (list2.size > 23) break
    }
    if (list2 != listOf<UInt>(3u, 4u, 5u)) {
        return "Wrong elements for (1u.toUByte() + 2u.toUByte()).toUByte()..(6u.toUByte() - 1u.toUByte()).toUByte(): $list2"
    }

    val list3 = ArrayList<UInt>()
    val range3 = (1u.toUShort() + 2u.toUShort()).toUShort()..(6u.toUShort() - 1u.toUShort()).toUShort()
    for (i in range3) {
        list3.add(i)
        if (list3.size > 23) break
    }
    if (list3 != listOf<UInt>(3u, 4u, 5u)) {
        return "Wrong elements for (1u.toUShort() + 2u.toUShort()).toUShort()..(6u.toUShort() - 1u.toUShort()).toUShort(): $list3"
    }

    val list4 = ArrayList<ULong>()
    val range4 = (1uL + 2uL)..(6uL - 1uL)
    for (i in range4) {
        list4.add(i)
        if (list4.size > 23) break
    }
    if (list4 != listOf<ULong>(3u, 4u, 5u)) {
        return "Wrong elements for (1uL + 2uL)..(6uL - 1uL): $list4"
    }

    return "OK"
}

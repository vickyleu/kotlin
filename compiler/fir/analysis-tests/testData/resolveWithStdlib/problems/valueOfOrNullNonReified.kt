// RUN_PIPELINE_TILL: BACKEND
enum class SomeEnum {
    ENTRY;
}

fun foo(s: String?) {
    val result = s?.let { valueOfOrNull(it) } ?: SomeEnum.ENTRY
    val result2 = s?.let { valueOfOrNull<SomeEnum>(it) } ?: SomeEnum.ENTRY
    val result3 = if (s == null) SomeEnum.ENTRY else valueOfOrNull(s)
    val result4 = if (s == null) SomeEnum.ENTRY else s.let { valueOfOrNull(it) }
}

fun <E : Enum<E>> valueOfOrNull(value: String): E? {
    return null
}

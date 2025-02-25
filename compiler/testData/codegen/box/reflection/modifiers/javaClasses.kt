// TARGET_BACKEND: JVM
// WITH_REFLECT
// JDK_KIND: FULL_JDK_17
// FILE: Interface.java
public interface Interface {
    int invoke(String s);
}

// FILE: J.java
public class J {
    public class Inner {}
    public static class Nested {}
}

// FILE: Record.java
public record Record(String value) {}

// FILE: box.kt

import kotlin.test.assertTrue
import kotlin.test.assertFalse

fun box(): String {
    assertFalse(Interface::class.isData)
    assertFalse(Interface::class.isInner)
    assertFalse(Interface::class.isCompanion)
    assertFalse(Interface::class.isFun)
    assertFalse(Interface::class.isValue)

    assertFalse(J.Nested::class.isData)
    assertFalse(J.Nested::class.isInner)
    assertFalse(J.Nested::class.isCompanion)
    assertFalse(J.Nested::class.isFun)
    assertFalse(J.Nested::class.isValue)

    assertFalse(J.Inner::class.isData)
    assertTrue(J.Inner::class.isInner)
    assertFalse(J.Inner::class.isCompanion)
    assertFalse(J.Inner::class.isFun)
    assertFalse(J.Inner::class.isValue)

    assertFalse(Record::class.isData)
    assertFalse(Record::class.isInner)
    assertFalse(Record::class.isCompanion)
    assertFalse(Record::class.isFun)
    assertFalse(Record::class.isValue)

    return "OK"
}

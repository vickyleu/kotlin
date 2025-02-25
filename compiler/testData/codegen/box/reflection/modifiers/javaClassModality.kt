// TARGET_BACKEND: JVM
// WITH_REFLECT
// JDK_KIND: FULL_JDK_17

// FILE: Sealed.java
public abstract sealed class Sealed permits Final, Open, Abstract {}

// FILE: Final.java
public final class Final extends Sealed {}

// FILE: Open.java
public non-sealed class Open extends Sealed {}

// FILE: Abstract.java
public non-sealed abstract class Abstract extends Sealed {}

// FILE: Interface.java
public interface Interface {}

// FILE: Anno.java
public @interface Anno {}

// FILE: E.java
public enum E {
    ENTRY {
        void foo() {}
    }
}

// FILE: box.kt
import kotlin.test.assertTrue
import kotlin.test.assertFalse

fun box(): String {
    assertTrue(Sealed::class.isSealed)
    assertFalse(Sealed::class.isFinal)
    assertFalse(Sealed::class.isOpen)
    assertFalse(Sealed::class.isAbstract)

    assertFalse(Final::class.isSealed)
    assertTrue(Final::class.isFinal)
    assertFalse(Final::class.isOpen)
    assertFalse(Final::class.isAbstract)

    assertFalse(Open::class.isSealed)
    assertFalse(Open::class.isFinal)
    assertTrue(Open::class.isOpen)
    assertFalse(Open::class.isAbstract)

    assertFalse(Abstract::class.isSealed)
    assertFalse(Abstract::class.isFinal)
    assertFalse(Abstract::class.isOpen)
    assertTrue(Abstract::class.isAbstract)

    assertFalse(Interface::class.isSealed)
    assertFalse(Interface::class.isFinal)
    assertFalse(Interface::class.isOpen)
    assertTrue(Interface::class.isAbstract)

    assertFalse(Anno::class.isSealed)
    assertTrue(Anno::class.isFinal)
    assertFalse(Anno::class.isOpen)
    assertFalse(Anno::class.isAbstract)

    assertFalse(E::class.isSealed)
    assertTrue(E::class.isFinal)
    assertFalse(E::class.isOpen)
    assertFalse(E::class.isAbstract)

    assertFalse(E.ENTRY::class.isSealed)
    assertTrue(E.ENTRY::class.isFinal)
    assertFalse(E.ENTRY::class.isOpen)
    assertFalse(E.ENTRY::class.isAbstract)

    return "OK"
}

// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// FILE: JavaInterface.java
import org.jetbrains.annotations.NotNull;
import java.util.List;

public interface JavaInterface<T> {
    @NotNull
    public List<@NotNull T> doSmth(@NotNull List<@NotNull T> x) { return null; }
}

// FILE: KotlinInterface.kt
interface KotlinInterface<T1> : JavaInterface<T1> {
    override fun do<caret>Smth(x: List<T1 & Any>): List<T1 & Any>
}

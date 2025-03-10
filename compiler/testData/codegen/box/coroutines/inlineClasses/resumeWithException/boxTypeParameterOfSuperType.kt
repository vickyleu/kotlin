// WITH_STDLIB

// Enable IR inliner on the first stage after KT-72296 is fixed
// LANGUAGE: -IrInlinerBeforeKlibSerialization
import kotlin.coroutines.*

interface EntityBase<out ID> {
    suspend fun id(): ID
}

inline class EntityId(val value: String)

interface Entity : EntityBase<EntityId>

var res = "FAIL"

class EntityStub : Entity {
    override suspend fun id(): EntityId = error("OK")
}

suspend fun test(): EntityId {
    val entity: Entity = EntityStub()
    return entity.id()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        res = it.exceptionOrNull()!!.message!!
    })
}

fun box(): String {
    builder {
        test().value
    }
    return res
}
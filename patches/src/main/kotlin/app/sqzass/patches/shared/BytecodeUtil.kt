package app.sqzass.patches.shared

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.builder.BuilderInstruction
import com.android.tools.smali.dexlib2.iface.Method

/**
 * Run [patchMethod] over every method of every class where [isCandidate] holds.
 *
 * The scan runs against immutable classes first; only classes with a match are
 * copied to mutable form, so unrelated DEX entries are not marked as modified.
 *
 * @param isCandidate Whether a method contains instructions [patchMethod] would change.
 * @param patchMethod Receives the mutable method and its instructions, returns the
 *   number of instruction sites it changed.
 * @return The total number of changed instruction sites.
 */
fun BytecodePatchContext.patchMatchingMethods(
    isCandidate: (Method) -> Boolean,
    patchMethod: MutableMethod.(List<BuilderInstruction>) -> Int,
): Int {
    val candidateTypes = mutableSetOf<String>()
    classDefForEach { classDef ->
        if (classDef.methods.any(isCandidate)) candidateTypes += classDef.type
    }

    var patched = 0
    for (type in candidateTypes) {
        for (method in mutableClassDefBy(type).methods) {
            val implementation = method.implementation ?: continue
            patched += method.patchMethod(implementation.instructions)
        }
    }
    return patched
}
/*
 * Copyright (C) 2026 sqzass
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.sqzass.patches.shared.misc.smali

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resource.resourceId
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.formatter.DexFormatter
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

internal fun descriptor(method: MethodReference): String = DexFormatter.INSTANCE.getMethodDescriptor(method)

internal fun descriptor(field: FieldReference): String = DexFormatter.INSTANCE.getFieldDescriptor(field)

internal fun String.toJavaClassName() = removePrefix("L").removeSuffix(";").replace('/', '.')

internal fun Method.parameterRegister(index: Int): String {
    val receiver = if (AccessFlags.STATIC.isSet(accessFlags)) 0 else 1
    val preceding = parameterTypes.take(index).sumOf { if (it == "J" || it == "D") 2 else 1 }
    return "p${receiver + preceding}"
}

private fun MutableClass.compileMethod(
    name: String,
    parameters: List<ImmutableMethodParameter>,
    returnType: String,
    accessFlags: Int,
    registers: Int,
    smali: String,
): MutableMethod {
    if (".catch" in smali) {
        throw PatchException("Try-catch blocks in raw smali strings are not supported by this helper. Use the dexlib2 API directly.")
    }
    
    val implementation = ImmutableMethodImplementation(registers).apply {
        addInstructionsWithLabels(0, smali)
    }

    return ImmutableMethod(
        type,
        name,
        parameters,
        returnType,
        accessFlags,
        null,
        null,
        implementation,
    ).toMutable()
}

internal fun MutableClass.addMethod(
    name: String,
    parameters: List<String>,
    returnType: String,
    accessFlags: Int,
    registers: Int,
    smali: String,
): MutableMethod = compileMethod(
    name,
    parameters.map { ImmutableMethodParameter(it, null, null) },
    returnType,
    accessFlags,
    registers,
    smali,
).also(methods::add)

internal fun MutableClass.addStaticMethod(
    name: String,
    parameters: List<String>,
    returnType: String,
    registers: Int,
    smali: String,
): String = descriptor(
    addMethod(name, parameters, returnType, AccessFlags.PUBLIC.value or AccessFlags.STATIC.value, registers, smali),
)

internal fun MutableClass.replaceStub(name: String, registers: Int, smali: String) {
    val stub = methods.single { it.name == name }
    methods.remove(stub)
    addStaticMethod(name, stub.parameterTypes.map(CharSequence::toString), stub.returnType, registers, smali)
}

internal fun MutableClass.addFieldReader(field: FieldReference): String {
    val (read, returnInstruction) = when {
        field.type == "Z" -> "iget-boolean" to "return"
        field.type == "I" -> "iget" to "return"
        field.type.startsWith("L") || field.type.startsWith("[") -> "iget-object" to "return-object"
        else -> throw PatchException("${descriptor(field)} has type ${field.type}, expected Z, I, an object or an array")
    }
    return descriptor(
        addMethod(
            "hxRead${field.name.replaceFirstChar(Char::uppercaseChar)}",
            emptyList(),
            field.type,
            AccessFlags.PUBLIC.value,
            2,
            """
                $read v0, p0, ${descriptor(field)}
                $returnInstruction v0
            """,
        ),
    )
}

internal fun MutableClass.replaceResourceIdStubs(type: ResourceType, stubs: Map<String, String>) =
    stubs.forEach { (stub, resourceName) ->
        replaceStub(
            stub,
            1,
            """
                const v0, ${resourceId(type, resourceName)}
                return v0
            """,
        )
    }

internal fun MutableClass.replaceMethod(name: String, registers: Int, smali: String) {
    val original = methods.single { it.name == name }
    methods.remove(original)
    methods.add(
        compileMethod(
            name,
            original.parameters.map { ImmutableMethodParameter.of(it) },
            original.returnType,
            original.accessFlags,
            registers,
            smali,
        ),
    )
}

internal fun MutableClass.addDefaultConstructor() {
    if (methods.any { it.name == "<init>" && it.parameterTypes.isEmpty() }) return
    methods.add(
        compileMethod(
            "<init>",
            emptyList(),
            "V",
            AccessFlags.PUBLIC.value or AccessFlags.CONSTRUCTOR.value,
            1,
            "invoke-direct {p0}, Ljava/lang/Object;-><init>()V\nreturn-void",
        ),
    )
}
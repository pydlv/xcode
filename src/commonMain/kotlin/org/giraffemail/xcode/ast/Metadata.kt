package org.giraffemail.xcode.ast

/**
 * Native Kotlin metadata storage - no string serialization
 */
sealed class NativeMetadata

/**
 * Function metadata with native TypeInfo objects
 */
data class FunctionMetadata(
    val returnType: TypeInfo = CanonicalTypes.Void,
    val paramTypes: Map<String, TypeInfo> = emptyMap(),
    val individualParamMetadata: Map<String, Map<String, String>> = emptyMap() // param name -> additional metadata
) : NativeMetadata()

/**
 * Variable assignment metadata with native TypeInfo
 */
data class VariableMetadata(
    val variableType: TypeInfo = CanonicalTypes.Unknown,
    val variableName: String? = null  // Name of the variable this metadata refers to
) : NativeMetadata()

/**
 * Class metadata with native TypeInfo
 */
data class ClassMetadata(
    val classType: TypeInfo = CanonicalTypes.Any,
    val methods: List<String> = emptyList()
) : NativeMetadata()

/**
 * Code with native metadata - no string serialization involved
 */
data class CodeWithNativeMetadata(
    val code: String,
    val metadata: List<NativeMetadata>
)

/**
 * Utilities for native metadata handling
 */
object NativeMetadataUtils {
    
    /**
     * Creates a CodeWithNativeMetadata object from code and native metadata
     */
    fun createCodeWithMetadata(code: String, metadata: List<NativeMetadata>): CodeWithNativeMetadata {
        return CodeWithNativeMetadata(
            code = code,
            metadata = metadata
        )
    }
    
    /**
     * Filter function metadata from a list of native metadata
     */
    fun filterFunctionMetadata(metadata: List<NativeMetadata>): List<FunctionMetadata> {
        return metadata.filterIsInstance<FunctionMetadata>()
    }
    
    /**
     * Filter variable metadata from a list of native metadata
     */
    fun filterVariableMetadata(metadata: List<NativeMetadata>): List<VariableMetadata> {
        return metadata.filterIsInstance<VariableMetadata>()
    }
    
    /**
     * Filter class metadata from a list of native metadata
     */
    fun filterClassMetadata(metadata: List<NativeMetadata>): List<ClassMetadata> {
        return metadata.filterIsInstance<ClassMetadata>()
    }
}
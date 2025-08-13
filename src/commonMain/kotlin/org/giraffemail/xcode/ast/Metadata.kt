package org.giraffemail.xcode.ast

/**
 * Legacy metadata storage with strings - kept for backward compatibility
 * TODO: Migrate to NativeMetadata for new code
 */
data class LanguageMetadata(
    val returnType: String? = null,
    val paramTypes: Map<String, String> = emptyMap(),
    val variableType: String? = null,
    val individualParamMetadata: Map<String, Map<String, String>> = emptyMap(), // param name -> metadata map
    val classType: String? = null,
    val classMethods: List<String> = emptyList()
)

/**
 * Legacy code with metadata storage - kept for backward compatibility
 * TODO: Migrate to CodeWithNativeMetadata for new code
 */
data class CodeWithMetadata(
    val code: String,
    val metadata: List<LanguageMetadata>
)

/**
 * Legacy utilities for metadata part handling - kept for backward compatibility
 * TODO: Migrate to NativeMetadataUtils for new code
 */
object MetadataSerializer {
    
    /**
     * Creates a CodeWithMetadata object from code and metadata parts
     */
    fun createCodeWithMetadata(code: String, metadata: List<LanguageMetadata>): CodeWithMetadata {
        return CodeWithMetadata(
            code = code,
            metadata = metadata
        )
    }
}

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
    val variableType: TypeInfo = CanonicalTypes.Unknown
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
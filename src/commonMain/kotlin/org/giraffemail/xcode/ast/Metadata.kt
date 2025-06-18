package org.giraffemail.xcode.ast

/**
 * Data classes for metadata storage
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
 * Data class representing code and metadata as separate parts
 * Note: metadata is stored as Kotlin objects, not serialized strings
 */
data class CodeWithMetadata(
    val code: String,
    val metadata: List<LanguageMetadata>
)

/**
 * Utilities for metadata part handling
 */
object MetadataSerializer {
    
    /**
     * Creates a CodeWithMetadata object from code and metadata parts
     * Note: metadata is stored as Kotlin objects, not serialized
     */
    fun createCodeWithMetadata(code: String, metadata: List<LanguageMetadata>): CodeWithMetadata {
        return CodeWithMetadata(
            code = code,
            metadata = metadata
        )
    }
}
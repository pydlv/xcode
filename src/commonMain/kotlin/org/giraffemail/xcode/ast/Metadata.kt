package org.giraffemail.xcode.ast

import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Data classes for metadata serialization using kotlinx.serialization
 */
@Serializable
data class LanguageMetadata(
    val returnType: String? = null,
    val paramTypes: Map<String, String> = emptyMap(),
    val variableType: String? = null,
    val individualParamMetadata: Map<String, Map<String, String>> = emptyMap() // param name -> metadata map
)

/**
 * Data class representing code and metadata as separate parts
 */
data class CodeWithMetadata(
    val code: String,
    val metadata: List<LanguageMetadata>
)

/**
 * Utilities for serializing and deserializing AST metadata using kotlinx.serialization
 */
object MetadataSerializer {
    
    private const val METADATA_PREFIX = "__META__:"
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }
    
    /**
     * Serializes language metadata to a JSON string
     */
    fun serialize(metadata: LanguageMetadata): String {
        return json.encodeToString(metadata)
    }
    
    /**
     * Deserializes language metadata from a JSON string
     */
    fun deserialize(jsonString: String): LanguageMetadata? {
        return try {
            json.decodeFromString<LanguageMetadata>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extracts metadata JSON from a metadata comment token
     */
    fun extractMetadataFromComment(commentText: String): LanguageMetadata? {
        val metadataIndex = commentText.indexOf(METADATA_PREFIX)
        if (metadataIndex == -1) return null
        
        val jsonStart = commentText.indexOf('{', metadataIndex)
        val jsonEnd = commentText.lastIndexOf('}')
        if (jsonStart == -1 || jsonEnd == -1 || jsonStart >= jsonEnd) return null
        
        val jsonStr = commentText.substring(jsonStart, jsonEnd + 1)
        return deserialize(jsonStr)
    }
    
    /**
     * Creates a metadata comment string for the given language
     */
    fun createMetadataComment(metadata: LanguageMetadata, language: String): String {
        val jsonStr = serialize(metadata)
        return when (language.lowercase()) {
            "python" -> "# $METADATA_PREFIX $jsonStr"
            else -> "// $METADATA_PREFIX $jsonStr"
        }
    }
    
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
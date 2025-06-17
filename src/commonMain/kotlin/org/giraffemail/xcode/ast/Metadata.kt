package org.giraffemail.xcode.ast

import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Data classes for metadata serialization using kotlinx.serialization
 */
@Serializable
data class TypescriptMetadata(
    val returnType: String? = null,
    val paramTypes: Map<String, String> = emptyMap(),
    val variableType: String? = null,
    val individualParamMetadata: Map<String, Map<String, String>> = emptyMap() // param name -> metadata map
)

/**
 * Utilities for serializing and deserializing AST metadata using kotlinx.serialization
 */
object MetadataSerializer {
    
    private const val METADATA_PREFIX = "__TS_META__:"
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }
    
    /**
     * Serializes TypeScript metadata to a JSON string
     */
    fun serialize(metadata: TypescriptMetadata): String {
        return json.encodeToString(metadata)
    }
    
    /**
     * Deserializes TypeScript metadata from a JSON string
     */
    fun deserialize(jsonString: String): TypescriptMetadata? {
        return try {
            json.decodeFromString<TypescriptMetadata>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extracts metadata JSON from a metadata comment token
     */
    fun extractMetadataFromComment(commentText: String): TypescriptMetadata? {
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
    fun createMetadataComment(metadata: TypescriptMetadata, language: String): String {
        val jsonStr = serialize(metadata)
        return when (language.lowercase()) {
            "python" -> "# $METADATA_PREFIX $jsonStr"
            else -> "// $METADATA_PREFIX $jsonStr"
        }
    }
}
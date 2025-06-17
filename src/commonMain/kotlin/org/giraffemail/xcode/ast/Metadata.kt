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
 * Utilities for serializing and deserializing AST metadata using kotlinx.serialization
 */
object MetadataSerializer {
    
    private const val METADATA_PREFIX = "__META__:"
    private const val METADATA_FILE_EXTENSION = ".meta"
    
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
     * Generates the metadata file path for a given source file path
     */
    fun getMetadataFilePath(sourceFilePath: String): String {
        return "$sourceFilePath$METADATA_FILE_EXTENSION"
    }
    
    /**
     * Writes metadata to a companion metadata file
     */
    fun writeMetadataToFile(sourceFilePath: String, metadata: List<LanguageMetadata>): Boolean {
        return try {
            val metadataFilePath = getMetadataFilePath(sourceFilePath)
            val jsonContent = json.encodeToString(metadata)
            // Note: In a real implementation, this would write to actual files
            // For now, we'll store in a map for testing purposes
            metadataFileStore[metadataFilePath] = jsonContent
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Reads metadata from a companion metadata file
     */
    fun readMetadataFromFile(sourceFilePath: String): List<LanguageMetadata> {
        return try {
            val metadataFilePath = getMetadataFilePath(sourceFilePath)
            val jsonContent = metadataFileStore[metadataFilePath]
            if (jsonContent != null) {
                json.decodeFromString<List<LanguageMetadata>>(jsonContent)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Checks if a metadata file exists for a given source file
     */
    fun hasMetadataFile(sourceFilePath: String): Boolean {
        val metadataFilePath = getMetadataFilePath(sourceFilePath)
        return metadataFileStore.containsKey(metadataFilePath)
    }
    
    /**
     * Temporary in-memory storage for metadata files during testing
     * In a real implementation, this would be actual file I/O
     */
    private val metadataFileStore = mutableMapOf<String, String>()
    
    /**
     * Clear the metadata file store (for testing purposes)
     */
    fun clearMetadataFileStore() {
        metadataFileStore.clear()
    }
}
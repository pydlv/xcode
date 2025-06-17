package org.giraffemail.xcode.ast

/**
 * Utilities for serializing and deserializing AST metadata to/from code comments.
 * This enables preservation of type information and other metadata when transpiling
 * through languages that don't support it natively.
 */
object MetadataUtils {
    
    private const val METADATA_PREFIX = "__TS_META__:"
    private const val COMMENT_PREFIX = "// "
    
    /**
     * Serializes metadata to a comment string.
     * @param metadata The metadata map to serialize
     * @return A comment string containing the serialized metadata, or empty string if no metadata
     */
    fun serializeToComment(metadata: Map<String, Any>?): String {
        if (metadata.isNullOrEmpty()) return ""
        
        val serialized = buildString {
            append("{")
            metadata.entries.joinTo(this, ",") { (key, value) ->
                "\"$key\":${serializeValue(value)}"
            }
            append("}")
        }
        
        return " $COMMENT_PREFIX$METADATA_PREFIX $serialized"
    }
    
    /**
     * Deserializes metadata from a comment string.
     * @param comment The comment string to parse
     * @return The deserialized metadata map, or null if no metadata found
     */
    fun deserializeFromComment(comment: String): Map<String, Any>? {
        val trimmed = comment.trim()
        
        // Check if this is a metadata comment
        if (!trimmed.startsWith(COMMENT_PREFIX + METADATA_PREFIX)) {
            return null
        }
        
        // Extract the JSON part
        val jsonStart = trimmed.indexOf('{')
        val jsonEnd = trimmed.lastIndexOf('}')
        if (jsonStart == -1 || jsonEnd == -1 || jsonStart >= jsonEnd) {
            return null
        }
        
        val jsonStr = trimmed.substring(jsonStart, jsonEnd + 1)
        return parseSimpleJson(jsonStr)
    }
    
    /**
     * Extracts metadata comments from a line of code.
     * @param codeLine A line of code that may contain metadata comments
     * @return Pair of (code without metadata comments, metadata map or null)
     */
    fun extractMetadataFromLine(codeLine: String): Pair<String, Map<String, Any>?> {
        val metadataCommentIndex = codeLine.indexOf("$COMMENT_PREFIX$METADATA_PREFIX")
        if (metadataCommentIndex == -1) {
            return codeLine to null
        }
        
        val codeWithoutMetadata = codeLine.substring(0, metadataCommentIndex).trimEnd()
        val metadataComment = codeLine.substring(metadataCommentIndex)
        val metadata = deserializeFromComment(metadataComment)
        
        return codeWithoutMetadata to metadata
    }
    
    private fun serializeValue(value: Any): String {
        return when (value) {
            is String -> "\"${value.replace("\"", "\\\"")}\""
            is Number -> value.toString()
            is Boolean -> value.toString()
            is Map<*, *> -> {
                buildString {
                    append("{")
                    value.entries.joinTo(this, ",") { (k, v) ->
                        "\"$k\":${serializeValue(v ?: "null")}"
                    }
                    append("}")
                }
            }
            is List<*> -> {
                buildString {
                    append("[")
                    value.joinTo(this, ",") { serializeValue(it ?: "null") }
                    append("]")
                }
            }
            else -> "\"$value\""
        }
    }
    
    /**
     * Simple JSON parser for basic metadata structures.
     * Only handles simple objects with string keys and string/number/boolean values.
     */
    private fun parseSimpleJson(json: String): Map<String, Any>? {
        try {
            val trimmed = json.trim()
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
                return null
            }
            
            val content = trimmed.substring(1, trimmed.length - 1).trim()
            if (content.isEmpty()) {
                return emptyMap()
            }
            
            val result = mutableMapOf<String, Any>()
            val pairs = splitJsonPairs(content)
            
            for (pair in pairs) {
                val colonIndex = pair.indexOf(':')
                if (colonIndex == -1) continue
                
                val key = pair.substring(0, colonIndex).trim().removeSurrounding("\"")
                val valueStr = pair.substring(colonIndex + 1).trim()
                val value = parseJsonValue(valueStr)
                
                result[key] = value
            }
            
            return result
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun splitJsonPairs(content: String): List<String> {
        val pairs = mutableListOf<String>()
        var current = StringBuilder()
        var depth = 0
        var inString = false
        var escaped = false
        
        for (char in content) {
            when {
                escaped -> {
                    current.append(char)
                    escaped = false
                }
                char == '\\' -> {
                    current.append(char)
                    escaped = true
                }
                char == '"' -> {
                    current.append(char)
                    inString = !inString
                }
                inString -> {
                    current.append(char)
                }
                char == '{' || char == '[' -> {
                    current.append(char)
                    depth++
                }
                char == '}' || char == ']' -> {
                    current.append(char)
                    depth--
                }
                char == ',' && depth == 0 -> {
                    pairs.add(current.toString())
                    current = StringBuilder()
                }
                else -> {
                    current.append(char)
                }
            }
        }
        
        if (current.isNotEmpty()) {
            pairs.add(current.toString())
        }
        
        return pairs
    }
    
    private fun parseJsonValue(valueStr: String): Any {
        val trimmed = valueStr.trim()
        
        return when {
            trimmed == "null" -> "null"
            trimmed == "true" -> true
            trimmed == "false" -> false
            trimmed.startsWith("\"") && trimmed.endsWith("\"") -> 
                trimmed.substring(1, trimmed.length - 1).replace("\\\"", "\"")
            trimmed.startsWith("{") && trimmed.endsWith("}") -> 
                parseSimpleJson(trimmed) ?: trimmed
            trimmed.toIntOrNull() != null -> trimmed.toInt()
            trimmed.toDoubleOrNull() != null -> trimmed.toDouble()
            else -> trimmed
        }
    }
}
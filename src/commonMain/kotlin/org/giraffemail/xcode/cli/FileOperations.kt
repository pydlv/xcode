package org.giraffemail.xcode.cli

/**
 * Platform-specific file operations
 */
expect object FileOperations {
    fun readFileContent(path: String): String
    fun writeFileContent(path: String, content: String)
    fun fileExists(path: String): Boolean
}
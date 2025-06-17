package org.giraffemail.xcode.cli

import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
actual object FileOperations {
    actual fun readFileContent(path: String): String {
        val file = fopen(path, "r") ?: throw RuntimeException("Cannot open file: $path")
        
        try {
            // Get file size
            fseek(file, 0, SEEK_END)
            val size = ftell(file)
            fseek(file, 0, SEEK_SET)
            
            if (size <= 0) {
                return ""
            }
            
            // Read content
            return memScoped {
                val buffer = allocArray<ByteVar>(size + 1)
                val bytesRead = fread(buffer, 1u, size.toULong(), file)
                buffer[bytesRead.toInt()] = 0 // Null terminate
                buffer.toKString()
            }
        } finally {
            fclose(file)
        }
    }
    
    actual fun writeFileContent(path: String, content: String) {
        val file = fopen(path, "w") ?: throw RuntimeException("Cannot create file: $path")
        
        try {
            content.encodeToByteArray().usePinned { pinned ->
                val bytesWritten = fwrite(pinned.addressOf(0), 1u, content.length.toULong(), file)
                if (bytesWritten.toInt() != content.length) {
                    throw RuntimeException("Failed to write complete content to file: $path")
                }
            }
        } finally {
            fclose(file)
        }
    }
    
    actual fun fileExists(path: String): Boolean {
        return access(path, F_OK) == 0
    }
}
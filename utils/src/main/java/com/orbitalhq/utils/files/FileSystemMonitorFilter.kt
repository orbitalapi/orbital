package com.orbitalhq.utils.files

import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name

/**
 * Shared filtering logic for file system monitors to determine which files should be monitored.
 * This ensures consistent behavior across both polling and watching implementations.
 */
object FileSystemMonitorFilter {

    /**
     * File extensions that should be monitored for changes.
     */
    val monitoredSuffixes = listOf(
        "taxi",
        "conf",
        "kts", // nebula
        // Avro file types
        "avsc",
        // Proto file types
        "proto",
        // OAS file types
        "yaml",
        "json",
        // SOAP file types
        "wsdl",
        "xml"
    )

    /**
     * Patterns for files and directories that should be ignored.
     */
    private val ignoredPatterns = listOf(
        ".git",           // Git directory
        ".swp",           // Vim swap files
        ".tmp",           // Temporary files
        "~",              // Backup files
    )

    /**
     * Determines if a file should be monitored based on its path.
     *
     * @param path The path to check
     * @param excludedDirectoryNames Additional directory names to exclude (e.g., "node_modules", "build")
     * @return true if the file should be monitored, false otherwise
     */
    fun shouldMonitor(path: Path, excludedDirectoryNames: List<String> = emptyList()): Boolean {
        val fileName = path.fileName?.toString() ?: return false

        // Check if in an excluded directory
        if (excludedDirectoryNames.contains(fileName)) {
            return false
        }

        // Ignore .git directory and anything under it
        if (path.any { it.fileName?.toString() == ".git" }) {
            return false
        }

        // Ignore macOS resource fork and temporary files (._*)
        if (fileName.startsWith("._")) {
            return false
        }

        // Ignore files matching any of the ignored patterns
        if (ignoredPatterns.any { pattern ->
            fileName.endsWith(pattern) || fileName.contains(pattern)
        }) {
            return false
        }

        // Only monitor files with the specified extensions
        val extension = path.extension
        return monitoredSuffixes.contains(extension)
    }

    /**
     * Determines if a file name (without path context) should be monitored.
     * This is useful when you only have the file name and not the full path.
     *
     * @param fileName The file name to check
     * @param extension The file extension
     * @return true if the file should be monitored, false otherwise
     */
    fun shouldMonitorFileName(fileName: String, extension: String): Boolean {
        // Ignore macOS resource fork and temporary files (._*)
        if (fileName.startsWith("._")) {
            return false
        }

        // Ignore files matching any of the ignored patterns
        if (ignoredPatterns.any { pattern ->
            fileName.endsWith(pattern) || fileName.contains(pattern)
        }) {
            return false
        }

        // Only monitor files with the specified extensions
        return monitoredSuffixes.contains(extension)
    }
}

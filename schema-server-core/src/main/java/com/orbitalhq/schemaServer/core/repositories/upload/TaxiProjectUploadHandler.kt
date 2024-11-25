package com.orbitalhq.schemaServer.core.repositories.upload

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemaServer.packages.PackageType
import com.orbitalhq.schemaServer.repositories.AddFileProjectRequest
import com.orbitalhq.toVynePackageIdentifier
import lang.taxi.packages.TaxiPackageProject
import org.springframework.stereotype.Component
import java.nio.file.Path
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import java.io.IOException
import java.util.zip.ZipException
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream


/**
 * Handles a zipped taxi project uploaded.
 *
 * Will handle either:
 *  - A zip containing a taxi project
 *  - A zip containing a parent directory, with the taxi project inside that directory
 */
@Component
class TaxiProjectUploadHandler() : ProjectUploadHandler, PackageIdentifyingUploadHandler {

   override val packageType: PackageType = PackageType.Taxi

   override fun readPackageIdentifier(payload: ByteArray): PackageIdentifier {
      val zipContents = validateAndAnalyzeZip(payload)

      // We know the zip is valid and contains a taxi.conf at this point
      val taxiConfEntry = zipContents.taxiConfEntries.first()

      return ByteArrayInputStream(payload).use { byteStream ->
         ZipInputStream(byteStream).use { zipStream ->
            extractPackageIdentifier(zipStream, taxiConfEntry)
         }
      }
   }

   override fun processUpload(
      packageId: PackageIdentifier,
      requestParams: Map<String, List<String>>,
      payload: ByteArray,
      projectRoot: Path
   ): AddFileProjectRequest {
      val zipContents = validateAndAnalyzeZip(payload)

      // Unzip contents to project root
      ByteArrayInputStream(payload).use { byteStream ->
         ZipInputStream(byteStream).use { zipStream ->
            unzipContents(zipStream, projectRoot)
         }
      }

      // Determine the actual project directory (accounting for wrapper directory)
      val projectDir = determineProjectDirectory(projectRoot, zipContents)

      return AddFileProjectRequest(
         path = projectDir.toAbsolutePath().toString(),
         isEditable = true
      )
   }

   private fun validateAndAnalyzeZip(payload: ByteArray): ZipContents {
      if (payload.size < 4) {
         throw InvalidZipException("File too small to be a valid ZIP")
      }

      // Check ZIP magic number (PK\x03\x04)
      if (payload[0] != 0x50.toByte() ||
         payload[1] != 0x4B.toByte() ||
         payload[2] != 0x03.toByte() ||
         payload[3] != 0x04.toByte()
      ) {
         throw InvalidZipException("Not a valid ZIP file - invalid magic number")
      }

      return ByteArrayInputStream(payload).use { byteStream ->
         try {
            ZipInputStream(byteStream).use { zipStream ->
               analyzeZipContents(zipStream)
            }
         } catch (e: ZipException) {
            throw InvalidZipException("Invalid ZIP file structure: ${e.message}")
         } catch (e: IOException) {
            throw InvalidZipException("Error reading ZIP file: ${e.message}")
         }
      }
   }

   private fun analyzeZipContents(zipStream: ZipInputStream): ZipContents {
      val entries = mutableListOf<String>()
      val taxiConfEntries = mutableListOf<String>()
      val directories = mutableSetOf<String>()

      var entry = zipStream.nextEntry
      while (entry != null) {
         val entryName = entry.name

         entries.add(entryName)

         if (entry.isDirectory) {
            directories.add(entryName)
         } else if (entryName.endsWith("taxi.conf")) {
            taxiConfEntries.add(entryName)
         }

         entry = zipStream.nextEntry
      }

      if (taxiConfEntries.isEmpty()) {
         throw MissingConfigurationException("taxi.conf file not found in zip")
      }

      if (taxiConfEntries.size > 1) {
         throw AmbiguousConfigurationException(
            "Multiple taxi.conf files found: ${taxiConfEntries.joinToString(", ")}"
         )
      }

      // Determine if there's a wrapper directory
      val hasWrapperDir = determineIfHasWrapperDirectory(entries, directories)
      val wrapperDir = if (hasWrapperDir) {
         entries.first().substringBefore("/")
      } else null

      return ZipContents(
         entries = entries,
         taxiConfEntries = taxiConfEntries,
         directories = directories,
         wrapperDirectory = wrapperDir
      )
   }

   private fun determineIfHasWrapperDirectory(
      entries: List<String>,
      directories: Set<String>
   ): Boolean {
      if (entries.isEmpty()) return false

      // Check if all entries start with the same directory
      val firstEntry = entries.first()
      val potentialWrapper = firstEntry.substringBefore("/")

      return potentialWrapper.isNotEmpty() &&
         entries.all { it.startsWith("$potentialWrapper/") } &&
         directories.contains("$potentialWrapper/")
   }

   private fun extractPackageIdentifier(
      zipStream: ZipInputStream,
      taxiConfEntry: String
   ): PackageIdentifier {
      var entry = zipStream.nextEntry
      while (entry != null) {
         if (entry.name == taxiConfEntry) {
            val configContent = zipStream.bufferedReader().use { it.readText() }
            try {
               val config = ConfigFactory.parseString(configContent)
               return config.extract<TaxiPackageProject>().identifier.toVynePackageIdentifier()
            } catch (e: Exception) {
               throw InvalidConfigurationException("Failed to parse taxi.conf: ${e.message}")
            }
         }
         entry = zipStream.nextEntry
      }
      throw IllegalStateException("Failed to find taxi.conf entry that was previously found")
   }

   private fun unzipContents(zipStream: ZipInputStream, targetDir: Path) {
      var entry = zipStream.nextEntry
      while (entry != null) {
         val entryPath = targetDir.resolve(entry.name)

         // Ensure we're not writing outside the target directory (zip slip vulnerability)
         if (!entryPath.normalize().startsWith(targetDir.normalize())) {
            throw SecurityException("ZIP entry '${entry.name}' would extract outside target directory")
         }

         if (entry.isDirectory) {
            entryPath.createDirectories()
         } else {
            // Ensure parent directories exist
            entryPath.parent?.createDirectories()

            // Write file contents
            entryPath.outputStream().use { output ->
               zipStream.copyTo(output)
            }
         }

         entry = zipStream.nextEntry
      }
   }

   private fun determineProjectDirectory(projectRoot: Path, zipContents: ZipContents): Path {
      return if (zipContents.wrapperDirectory != null) {
         projectRoot.resolve(zipContents.wrapperDirectory)
      } else {
         projectRoot
      }
   }

   private data class ZipContents(
      val entries: List<String>,
      val taxiConfEntries: List<String>,
      val directories: Set<String>,
      val wrapperDirectory: String?
   )
}

// Custom exceptions for different error scenarios
class InvalidZipException(message: String) : Exception(message)
class MissingConfigurationException(message: String) : Exception(message)
class InvalidConfigurationException(message: String) : Exception(message)
class AmbiguousConfigurationException(message: String) : Exception(message)

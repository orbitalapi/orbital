package com.orbitalhq.internal.packagePublisher

import com.orbitalhq.cockpit.core.schemas.BuiltInTypesProvider
import picocli.CommandLine
import java.nio.file.FileSystems
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeLines
import kotlin.io.path.writeText

/**
 * Writes out the sources of the built-in Taxi projects
 * to a taxi project
 */
class BuiltInsSourcePackageWriter(
   private val config: OrbitalPackagePublisherConfig,
   private val commandLine: CommandLine) {

   private fun printOut(message: String) = commandLine.out.println(message)

   fun write() {
      val gitRepo = GitTransport(config, commandLine)
      updateVersion()
      writeSources()
      gitRepo.commitAndTryPush()
   }

   private fun updateVersion() {
      val basePath = Paths.get(config.outputPath)
      val taxiConfFile = basePath.resolve("taxi.conf")
      printOut("Updating version in $${taxiConfFile} to ${config.version}")
      if (taxiConfFile.exists()) {
         val taxiConfSrc = taxiConfFile.readText()
         val lines = taxiConfSrc.lines().toMutableList()
         val versionLine = taxiConfSrc.lines()
            .indexOfFirst { it.startsWith("version:") }
         lines[versionLine] = "version: ${config.version}"
         taxiConfFile.writeText(lines.joinToString("\n"))
      } else {
         taxiConfFile.toFile().parentFile.mkdirs()
         taxiConfFile.toFile().createNewFile()
         taxiConfFile.writeLines(listOf(
            "name: com.orbitalhq/core",
            "version: ${config.version}",
            "sourceRoot: src/"
         ))
      }
   }

   private fun writeSources() {
      val basePath = Paths.get(config.outputPath)
      printOut("Writing sources using base path of $basePath")
      BuiltInTypesProvider.sourcePackage
         .sources.forEach { versionedSource ->
            val separator = FileSystems.getDefault().separator
            val filename = versionedSource.name.replace(".", separator)
            val filePath = Paths.get(basePath.toString(), "${separator}src${separator}", "${filename}.taxi")
            filePath.toFile().parentFile.mkdirs()
            printOut("Writing to $filePath")
            filePath.writeText(versionedSource.content)
         }
   }
}

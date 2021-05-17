package io.vyne.testcli.commands

import io.vyne.VersionedSource
import io.vyne.utils.log
import picocli.CommandLine
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.Callable

@CommandLine.Command(
   name = "extractSchema"
)
class ExtractSchemaCommand : Callable<Int> {
   @CommandLine.Option(
      names = ["-p", "--path"],
      defaultValue = "",
      description = ["The directory of schema.json file"]
   )
   lateinit var specPath: Path

   @CommandLine.Option(
      names = ["-o", "--outputPath"],
      defaultValue = "",
      description = ["The directory to contain extracted schema files"]
   )
   lateinit var outputPath: Path
   override fun call(): Int {
      val inputFolder = resolvePath(specPath)
      val outputFolder = resolvePath(outputPath, true)
//      val sources = QueryTester().fetchSources(inputFolder.toFile())
//      persistTestSchema(outputFolder, sources)
      TODO()
      return 1
   }

   private fun resolvePath(inputPath: Path, createFolderIsNotExists: Boolean = false): Path {
      val absoluteSpecPath = if (inputPath.isAbsolute) {
         inputPath
      } else {
         val currentDir = Paths.get(System.getProperty("user.dir"))
         log().debug("Launched from $currentDir")

         val resolvedPath = currentDir.resolve(inputPath)
         log().debug("Searching for tests from $resolvedPath")
         resolvedPath
      }

      if (createFolderIsNotExists && !absoluteSpecPath.toFile().exists()) {
         absoluteSpecPath.toFile().mkdirs()
         return absoluteSpecPath
      }

      return if (Files.isDirectory(absoluteSpecPath)) {
         absoluteSpecPath
      } else {
         absoluteSpecPath.parent
      }
   }

   private fun persistTestSchema(rootFolderPath: Path, versionedSources: List<VersionedSource>) {
      val rootFolderPath =  rootFolderPath.toFile().absolutePath
      versionedSources.forEach { versionedSource ->
         val parts = versionedSource.name.split("/")
         if (parts.size == 1) {
            Files.write(Paths.get(rootFolderPath, "${parts[0]}"), listOf(versionedSource.content), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
         } else {
            val fileName = parts.last()
            val subPaths = parts.subList(0, parts.size - 1)
            val folderFile = Paths.get(rootFolderPath, *subPaths.toTypedArray()).toFile()
            if (!folderFile.exists()) {
               folderFile.mkdirs()
            }
            val writePath = subPaths.plus(fileName)
            Files.write(Paths.get(rootFolderPath, *writePath.toTypedArray()), listOf(versionedSource.content), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
         }
      }
   }
}

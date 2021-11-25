package io.vyne.schemaServer.file

import io.vyne.VersionedSource
import io.vyne.schemaServer.VersionedSourceLoader
import io.vyne.spring.FileSystemSchemaLoader
import lang.taxi.packages.TaxiPackageProject
import java.nio.file.Path
import java.nio.file.Paths

class FileSystemVersionedSourceLoader(
   private val fileSystemSchemaLoader: FileSystemSchemaLoader
) : VersionedSourceLoader {
   override val identifier: String
      get() = fileSystemSchemaLoader.identifier

   override fun loadVersionedSources(forceVersionIncrement: Boolean, cachedValuePermissible: Boolean): List<VersionedSource> {
      return fileSystemSchemaLoader.loadVersionedSources(forceVersionIncrement, cachedValuePermissible)
   }

   val projectPath: Path
    get() = fileSystemSchemaLoader.projectPath

   val projectAndRoot: Pair<TaxiPackageProject?, Path>
      get() = fileSystemSchemaLoader.projectAndRoot

   companion object {
      fun forProjectHome(projectHome: String): FileSystemVersionedSourceLoader {
         return FileSystemVersionedSourceLoader(FileSystemSchemaLoader(Paths.get(projectHome)))
      }
   }


}

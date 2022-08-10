package io.vyne.schemaServer.core.file

import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schema.publisher.loaders.FileSystemSchemaProjectLoader
import lang.taxi.packages.TaxiPackageProject
import java.nio.file.Path
import java.nio.file.Paths

class FileSystemVersionedSourceLoader(
   private val fileSystemSchemaLoader: FileSystemSchemaProjectLoader
) : io.vyne.schemaServer.core.VersionedSourceLoader {
   override val identifier: String
      get() = fileSystemSchemaLoader.identifier

   override fun loadSourcePackage(forceVersionIncrement: Boolean, cachedValuePermissible: Boolean): SourcePackage {
      return fileSystemSchemaLoader.loadVersionedSources(forceVersionIncrement, cachedValuePermissible)
   }

   val projectPath: Path
    get() = fileSystemSchemaLoader.projectPath

   val projectAndRoot: Pair<TaxiPackageProject?, Path>
      get() = fileSystemSchemaLoader.projectAndRoot

   companion object {
      fun forProjectHome(projectHome: String): FileSystemVersionedSourceLoader {
         return FileSystemVersionedSourceLoader(FileSystemSchemaProjectLoader(Paths.get(projectHome)))
      }
   }


}

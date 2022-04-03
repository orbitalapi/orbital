package io.vyne.schemaPublisherApi.loaders

import io.vyne.VersionedSource
import lang.taxi.sources.SourceCode
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.isDirectory

/**
 * Loads sources from a given path and emits them as taxi VersionedSources.
 * Allows for plugging different transpilation of schema formats (eg., Protobuf / Swagger),
 * as well as loading taxi directly.
 *
 * Implementations must provide a no-args constrcutor.
 */
interface SchemaSourcesLoader {
   fun load(paths: List<Path>): List<VersionedSource>
}


/**
 * The default implementation.
 * Just a very tiny wrapper around the existing FileSystemSchemaLoader
 */
@OptIn(ExperimentalPathApi::class)
class FileSystemSourcesLoader : SchemaSourcesLoader {
   override fun load(paths: List<Path>): List<VersionedSource> {
      return paths.flatMap { load(it) }
   }

   fun load(path: Path): List<VersionedSource> {
      when {
         path.isDirectory() -> {
            val fileSystemVersionedSourceLoader = FileSystemSchemaProjectLoader(path)
            fileSystemVersionedSourceLoader.loadVersionedSources(
               forceVersionIncrement = false,
               cachedValuePermissible = false
            )
         }
         else -> {
            return LegacyFileSourceProvider().load(listOf(path))
         }
      }

      return FileSystemSchemaProjectLoader(path).loadVersionedSources(
         forceVersionIncrement = false,
         cachedValuePermissible = false
      )
   }

}

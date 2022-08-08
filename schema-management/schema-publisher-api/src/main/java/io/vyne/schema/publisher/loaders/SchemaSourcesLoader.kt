package io.vyne.schema.publisher.loaders

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
 */

@Deprecated("Use a SchemaPackageLoader")
interface SchemaSourcesLoader {
   fun load(): List<VersionedSource>
}


/**
 * The default implementation.
 * Just a very tiny wrapper around the existing FileSystemSchemaLoader
 */
@OptIn(ExperimentalPathApi::class)
class FileSystemSourcesLoader(private val paths: List<Path>) : SchemaSourcesLoader {
   constructor(path: Path) : this(listOf(path))

   override fun load(): List<VersionedSource> {
      return paths.flatMap { load(it) }
   }

   private fun load(path: Path): List<VersionedSource> {
      when {
         path.isDirectory() -> {
            val fileSystemVersionedSourceLoader = FileSystemSchemaProjectLoader(path)
            fileSystemVersionedSourceLoader.loadVersionedSources(
               forceVersionIncrement = false,
               cachedValuePermissible = false
            )
         }
         else -> {
            return LegacyFileSourceProvider(listOf(path)).load()
         }
      }

      return FileSystemSchemaProjectLoader(path).loadVersionedSources(
         forceVersionIncrement = false,
         cachedValuePermissible = false
      )
   }

}

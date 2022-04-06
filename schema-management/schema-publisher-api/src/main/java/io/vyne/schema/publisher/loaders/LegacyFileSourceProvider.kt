package io.vyne.schema.publisher.loaders

import io.vyne.VersionedSource
import lang.taxi.sources.SourceCode
import java.nio.file.Path

/**
 * Source loader that supports loading taxi files directly,
 * outside of a taxi project.
 *
 * This approach is discouraged - consider declaring an actual taxi
 * project with a taxi.conf file
 */
class LegacyFileSourceProvider : SchemaSourcesLoader {

   override fun load(paths: List<Path>): List<VersionedSource> {
      return paths.map { path ->
         val content = path.toFile().readText()
         VersionedSource.fromTaxiSourceCode(SourceCode(path.toFile().name, content, path))
      }

   }
}

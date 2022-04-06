package io.vyne.schema.spring.loaders

import io.vyne.VersionedSource
import io.vyne.schema.publisher.loaders.SchemaSourcesLoader
import lang.taxi.generators.protobuf.TaxiGenerator
import lang.taxi.sources.SourceCode
import okio.FileSystem
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.name

// This sits in the spring project, as it has referecnes to spring classes.
// Can rethink this later if needed.
@OptIn(ExperimentalPathApi::class)
class ProtobufSourcesLoader : SchemaSourcesLoader {
   override fun load(paths: List<Path>): List<VersionedSource> {
//      val resolver = PathMatchingResourcePatternResolver(this::class.java.classLoader)
//      val resources = resolver.getResources(path.toString()).toList()

      // TODO: Find the common path.  Also, how does this work with Classpath?
      // For now, just use the first.
//      if (resources.isEmpty()) {
//         return emptyList()
//      }
      val parentPath = paths.first().parent

      val generated = TaxiGenerator().addSchemaRoot(parentPath.toString())
         .generate()
      val sources =
         generated.taxi.map { VersionedSource.fromTaxiSourceCode(SourceCode(parentPath.name, it, parentPath)) }
      return sources

   }
}

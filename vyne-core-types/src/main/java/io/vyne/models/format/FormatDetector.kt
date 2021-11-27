package io.vyne.models.format

import com.google.common.cache.CacheBuilder
import io.vyne.schemas.Metadata
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Type

/**
 * Looks at the annotations on a type and finds a matching ModelFormatParser if present
 */
class FormatDetector(specs: List<ModelFormatSpec>) {
   companion object {
      private val formatDetectors = CacheBuilder
         .newBuilder()
         .build<List<ModelFormatSpec>,FormatDetector>()

      /**
       * Caches the FormatDetector - as outside of tests, these
       * very rarely change at runtime
       */
      fun get(specs:List<ModelFormatSpec>): FormatDetector {
         return formatDetectors.get(specs) {
            FormatDetector(specs)
         }
      }
   }
   private val specsByAnnotationName: Map<QualifiedName, ModelFormatSpec> =
      specs.flatMap { parser -> parser.annotations.map { annotation -> annotation to parser } }
         .toMap()

   fun getFormatType(type: Type): Pair<Metadata,ModelFormatSpec>? {
      if (type.isCollection) {
         return getFormatType(type.collectionType!!)
      }
      val matchedParser = type.metadata.firstOrNull { specsByAnnotationName.contains(it.name) }
         ?.let { metadata -> metadata to specsByAnnotationName.getValue(metadata.name)  }
      if (matchedParser != null) {
         return matchedParser
      }
      // Find the first superType that we inherit from which has a format type
      return type.inherits
         .asSequence()
         .map { superType -> getFormatType(superType) }
         .filterNotNull()
         .firstOrNull()
   }
}

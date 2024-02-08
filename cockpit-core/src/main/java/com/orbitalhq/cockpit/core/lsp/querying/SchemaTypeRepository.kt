package com.orbitalhq.cockpit.core.lsp.querying

import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.toTaxiQualifiedName
import lang.taxi.lsp.completion.TypeRepository
import lang.taxi.types.AnnotationType
import lang.taxi.types.EnumType
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type

/**
 * A type repository used for query building.
 *
 * When authoring queries in the UI, the schema is stable (vs authoring schemas in VSCode)
 */
class SchemaTypeRepository(private val schema: Schema) : TypeRepository {
   override fun getTypeNames(): List<Pair<QualifiedName, Type?>> {
      return schema.types.map { it.name.toTaxiQualifiedName() to it.taxiType }
   }

   override fun getTypeName(text: String): QualifiedName? {
      return if (schema.hasType(text)) {
         schema.type(text).name.toTaxiQualifiedName()
      } else null
   }

   override fun getEnumType(fullyQualifiedName: String): EnumType? {
      return if (schema.hasType(fullyQualifiedName)) {
         val type = schema.type(fullyQualifiedName).taxiType
         return if (type is EnumType) {
            type
         } else null
      } else null
   }

   override fun annotation(annotationName: String): AnnotationType? {
      return if (schema.hasType(annotationName)) {
         val type = schema.type(annotationName).taxiType
         return if (type is AnnotationType) {
            type
         } else null
      } else null
   }
}

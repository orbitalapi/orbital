package io.vyne.cask.services

import io.vyne.query.queryBuilders.VyneQlGrammar
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import lang.taxi.types.Annotation
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.ObjectTypeDefinition
import lang.taxi.types.Type
import org.springframework.stereotype.Service

@Service
class DefaultCaskTypeProvider {
   private val taxiDocument = mapOf("vyne.cask.types" to
      // Note: Currently including VyneQl type definitions in the cask schema.
      // This ultimately needs to move, but not sure where to yet.
      Compiler("""
         ${VyneQlGrammar.QUERY_TYPE_TAXI}
         namespace vyne.cask {
            type CaskInsertedAt inherits Instant
            type CaskMessageId inherits String
         }
      """.trimIndent()).compile())

   fun defaultCaskTaxiTypes(): Map<String, TaxiDocument> = taxiDocument
   fun vyneQlQueryType() = taxiDocument.values.first().type(VyneQlGrammar.QUERY_TYPE_NAME)
   fun insertedAtType() = taxiDocument.values.first().type("vyne.cask.CaskInsertedAt")
   fun caskMessageIdType() = taxiDocument.values.first().type("vyne.cask.CaskMessageId")
   fun withDefaultCaskTaxiType(type: Type): ObjectType {
      return ObjectType(
         "vyne.cask.${type.toQualifiedName().fullyQualifiedName}",
         ObjectTypeDefinition(
            inheritsFrom = setOf(type),
            fields = setOf(
               Field("caskInsertedAt", insertedAtType()),
               Field("caskMessageId", caskMessageIdType())
            ),
            compilationUnit = CompilationUnit.unspecified(),
            annotations = setOf(Annotation("Generated")),
            typeDoc = "Generated by Cask.  Source type is ${type.qualifiedName}}"
         )
      )
   }
}

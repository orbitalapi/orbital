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
import lang.taxi.types.View
import org.springframework.stereotype.Service

@Service
class DefaultCaskTypeProvider {
   private val taxiDocument = mapOf("${VyneCaskPrefix}types" to
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
   fun insertedAtType() = taxiDocument.values.first().type("${VyneCaskPrefix}CaskInsertedAt")
   fun caskMessageIdType() = taxiDocument.values.first().type("${VyneCaskPrefix}CaskMessageId")
   fun withDefaultCaskTaxiType(type: Type): ObjectType {
      return ObjectType(
         "$VyneCaskPrefix${type.toQualifiedName().fullyQualifiedName}",
         ObjectTypeDefinition(
            inheritsFrom = setOf(type),
            fields = setOf(
               Field(name = "caskInsertedAt", type = insertedAtType(), compilationUnit =  CompilationUnit.unspecified()),
               Field(name = "caskMessageId", type = caskMessageIdType(), compilationUnit =  CompilationUnit.unspecified())
            ),
            compilationUnit = CompilationUnit.unspecified(),
            annotations = setOf(Annotation("Generated")),
            typeDoc = "Generated by Cask.  Source type is ${type.qualifiedName}}"
         )
      )
   }

   companion object {
      const val VyneCaskPrefix = "vyne.cask."
   }
}

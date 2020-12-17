package io.vyne.queryService.schemas

import io.vyne.FactSets
import io.vyne.VersionedSource
import io.vyne.query.Fact
import lang.taxi.Compiler
import lang.taxi.generators.SchemaWriter

object VyneQueryBuiltInTypesProvider {
   private const val schemaName = "io.vyne.types"
   const val vyneNamespace = "io.vyne"
   const val vyneUserNameType = "Username"
   private val taxiDocument = Compiler("""
      namespace $vyneNamespace {
         type $vyneUserNameType inherits String
     }
      """.trimIndent()).compile()
   val versionedSources = SchemaWriter()
      .generateSchemas(listOf(taxiDocument))
      .mapIndexed { index, generatedSchema ->
      val versionedSourceName = if (index > 0) schemaName + index else schemaName
      VersionedSource(versionedSourceName, "1.0.0", generatedSchema)
   }

   fun vyneUserNameCallerFact(userName: String) = Fact("$vyneNamespace.$vyneUserNameType", userName, FactSets.CALLER)
}

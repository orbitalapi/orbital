package io.vyne.queryService.schemas

import io.vyne.VersionedSource
import io.vyne.queryService.security.VyneUser
import lang.taxi.Compiler
import lang.taxi.generators.SchemaWriter

object VyneTypes {
   const val NAMESPACE = "io.vyne"
}
object VyneQueryBuiltInTypesProvider {
   private const val schemaName = "io.vyne.types"

   private val builtInTypesSource = listOf(
      VyneUser.USERNAME_TYPEDEF
   ).joinToString("\n")
   private val taxiDocument = Compiler(builtInTypesSource).compile()
   val versionedSources = SchemaWriter()
      .generateSchemas(listOf(taxiDocument))
      .mapIndexed { index, generatedSchema ->
         val versionedSourceName = if (index > 0) schemaName + index else schemaName
         VersionedSource(versionedSourceName, "1.0.0", generatedSchema)
      }


}

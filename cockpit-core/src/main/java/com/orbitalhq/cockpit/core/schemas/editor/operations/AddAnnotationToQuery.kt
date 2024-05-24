package com.orbitalhq.cockpit.core.schemas.editor.operations

import arrow.core.Either
import com.orbitalhq.SourcePackage
import com.orbitalhq.annotations.http.HttpOperations
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.SchemaMemberKind
import com.orbitalhq.schemas.fqn
import lang.taxi.CompilationException
import lang.taxi.TaxiDocument
import lang.taxi.TaxiParser
import lang.taxi.annotations.HttpOperation
import lang.taxi.annotations.WebsocketOperation
import lang.taxi.source

data class AddWebsocketEndpointToQuery(
   override val queryQualifiedName: QualifiedName,
   val path: String,
) : AddAnnotationToQuery(EditKind.AddWebsocketEndpointToQuery) {
   override val annotation: String = """@WebsocketOperation(path = "$path")"""
   override val typeNamesToImport: List<String> = listOf(WebsocketOperation.NAME)
}
data class AddHttpEndpointToQuery(
   override val queryQualifiedName: QualifiedName,
   val path: String,
   val method: HttpOperations.HttpMethod
) : AddAnnotationToQuery(EditKind.AddHttpEndpointToQuery) {
   override val annotation: String = """@HttpOperation(method = "$method", url = "$path")"""
   override val typeNamesToImport: List<String> = listOf(HttpOperation.NAME)
}

abstract class AddAnnotationToQuery(override val editKind: EditKind) : SchemaEditOperation() {
   override fun applyTo(
      sourcePackage: SourcePackage,
      taxiDocument: TaxiDocument
   ): Either<CompilationException, SourceEditResult> {
      val compiler = buildCompiler(sourcePackage, taxiDocument)
      val (tokens, errors) = compiler.parseResult

      val matchingQueries = tokens.namedQueries.filter { it.second.queryName().identifier().text == queryQualifiedName.name }

      val (_, namedQueryToken) = when {
         matchingQueries.isEmpty() -> error("Could not find a query named $queryQualifiedName in the provided source")
         matchingQueries.size > 1 -> error("Found more than one query named $queryQualifiedName in the provided source")
         else -> matchingQueries.single()
      }

      return applyEditAndCompile(
         listOf(
            addAnnotation(namedQueryToken),
            addImports(typeNamesToImport, namedQueryToken)
         ), sourcePackage, taxiDocument
      )
//      return lines.joinToString("\n")
   }



   private fun addAnnotation(token: TaxiParser.NamedQueryContext): SourceEdit {
      return SourceEdit(
         sourceName = token.source().sourceName,
         range = token.asCharacterInsertionPoint(EditPosition.BeforePosition),
         newText = annotation + "\n"
      )
   }

   abstract val annotation: String
   abstract val queryQualifiedName: QualifiedName
   abstract val typeNamesToImport: List<String>


   override val loadExistingState: Boolean = false

   override fun calculateAffectedTypes(): List<Pair<SchemaMemberKind, QualifiedName>> {
      return listOf(SchemaMemberKind.QUERY to queryQualifiedName)
   }

}

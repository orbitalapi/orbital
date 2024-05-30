package com.orbitalhq.cockpit.core.schemas.editor.operations

import arrow.core.Either
import com.orbitalhq.SourcePackage
import com.orbitalhq.annotations.http.HttpOperations
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.SchemaMemberKind
import lang.taxi.CompilationException
import lang.taxi.TaxiDocument
import lang.taxi.TaxiParser
import lang.taxi.annotations.HttpOperation
import lang.taxi.annotations.WebsocketOperation
import lang.taxi.source

data class AddOrRemoveWebsocketEndpointAnnotation(
   override val queryQualifiedName: QualifiedName,
   val path: String,
   override val operation: Operation
) : AddOrRemoveAnnotationFromQuery(EditKind.AddOrRemoveWebsocketEndpointAnnotation) {
   override val annotation: String = """@WebsocketOperation(path = "$path")"""
   override val annotationName: String = "WebsocketOperation"
   override val typeNamesToImport: List<String> = listOf(WebsocketOperation.NAME)
}
data class AddOrRemoveHttpEndpointAnnotation(
   override val queryQualifiedName: QualifiedName,
   val path: String,
   val method: HttpOperations.HttpMethod,
   override val operation: Operation
) : AddOrRemoveAnnotationFromQuery(EditKind.AddOrRemoveHttpEndpointAnnotation) {
   override val annotation: String = """@HttpOperation(method = "$method", url = "$path")"""
   override val annotationName: String = "HttpOperation"
   override val typeNamesToImport: List<String> = listOf(HttpOperation.NAME)
}

abstract class AddOrRemoveAnnotationFromQuery(override val editKind: EditKind) : SchemaEditOperation() {
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

      val edit = when (operation) {
         Operation.Add -> addAnnotation(namedQueryToken)
         Operation.Remove -> removeAnnotation(namedQueryToken)
      }

      return applyEditAndCompile(
         edit,
         sourcePackage,
         taxiDocument
      )
   }

   private fun removeAnnotation(token: TaxiParser.NamedQueryContext): List<SourceEdit> {
      val matchingAnnotation = token.annotation()
         .filter { it.qualifiedName().text == annotationName }
      require (matchingAnnotation.size == 1) { "Expected a single annotation with name '$annotationName', but found ${matchingAnnotation.size}"}
      val annotation = matchingAnnotation.single()
      return listOf(SourceEdit(
         sourceName = token.source().sourceName,
         range = annotation.asCharacterPositionRange(),
         newText = ""
      ))
   }

   private fun addAnnotation(token: TaxiParser.NamedQueryContext): List<SourceEdit> {
      return listOf(
         SourceEdit(
            sourceName = token.source().sourceName,
            range = token.asCharacterInsertionPoint(EditPosition.BeforePosition),
            newText = annotation + "\n"
         ),
         addImports(typeNamesToImport, token)
      )
   }

   abstract val annotation: String
   abstract val annotationName: String
   abstract val queryQualifiedName: QualifiedName
   abstract val typeNamesToImport: List<String>
   abstract val operation: Operation


   override val loadExistingState: Boolean = false

   override fun calculateAffectedTypes(): List<Pair<SchemaMemberKind, QualifiedName>> {
      return listOf(SchemaMemberKind.QUERY to queryQualifiedName)
   }

   enum class Operation {
      Add, Remove
   }
}

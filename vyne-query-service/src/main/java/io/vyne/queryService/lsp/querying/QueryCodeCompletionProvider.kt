package io.vyne.queryService.lsp.querying

import io.vyne.query.graph.Algorithms
import io.vyne.schemas.*
import lang.taxi.TaxiParser.*
import lang.taxi.lsp.CompilationResult
import lang.taxi.lsp.completion.*
import lang.taxi.query.QueryMode
import lang.taxi.searchUpForRule
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.eclipse.lsp4j.*
import java.util.concurrent.CompletableFuture

/**
 * Completion provider focussed on delivering hints when writing queries
 */
class QueryCodeCompletionProvider(private val typeProvider: TypeProvider, private val schema: Schema) :
   CompletionProvider {

   private val topLevelQueryCompletionItems = listOf(
      "find" to "Query for a single item, or a list of items",
      "stream" to "Query for a continuous stream of data"
   )
      .map { (text, description) ->
         CompletionItem(text).apply {
            kind = CompletionItemKind.Keyword
            insertText = text
            detail = description
            additionalTextEdits = emptyList()
         }
      }

   override fun getCompletionsForContext(
      compilationResult: CompilationResult,
      params: CompletionParams,
      importDecorator: ImportCompletionDecorator,
      contextAtCursor: ParserRuleContext?,
      lastSuccessfulCompilation: CompilationResult?
   ): CompletableFuture<List<CompletionItem>> {
      if (contextAtCursor == null) {
         return completed(topLevelQueryCompletionItems)
      }

      val completions = when (contextAtCursor) {
         is TypeProjectionContext -> {
            if (contextAtCursor.stop.text == "as") {
               val queryTypeListContext =
                  contextAtCursor.searchUpForRule(QueryTypeListContext::class.java) as? ParserRuleContext?
               val children = queryTypeListContext?.children ?: emptyList()
               // If the source type is a collection...
               val sourceTypeIsCollection =
                  children.isNotEmpty() && children.filterIsInstance<FieldTypeDeclarationContext>().any {
                     it.optionalTypeReference()?.typeReference()?.arrayMarker() != null
                  }
               if (children.isNotEmpty()) {
                  buildAsCompletion(params, sourceTypeIsCollection)
               } else {
                  emptyList()
               }
            } else {
               emptyList()
            }
         }
         is SingleNamespaceDocumentContext -> topLevelQueryCompletionItems
         is QueryDirectiveContext -> {
            // This is a find { ... }, possibly with a given { .. }.
            // It's not a projection (yet).
            val typesInGivenClause = findTypesInGivenClause(contextAtCursor, compilationResult)
            if (typesInGivenClause.isNotEmpty()) {
               findModelsReturnsFromProvidedInputs(
                  importDecorator,
                  typesInGivenClause,
                  includeModelAttributes = typesInGivenClause.isNotEmpty() // exclude model attributes if we're just in a find { .. }
               )
            } else {
               val queryMode = findQueryMode(contextAtCursor)
               findModelsReturnableFromNoArgServices(importDecorator, queryMode) +
                  findModelsReturnableFromQueryOperations(importDecorator, queryMode)
            }
         }


         is ParameterConstraintContext -> {
            suggestFilterTypes(contextAtCursor, importDecorator, compilationResult)
         }
         is FieldDeclarationContext,
         is TypeBodyContext,
         is IdentifierContext,
         is QualifiedNameContext -> {
            // In tests, it looks like we could be either declaring a filter criteria here,
            // or listing fields we want to add to the query.
            if (contextAtCursor.searchUpForRule(ConditionalTypeStructureDeclarationContext::class.java) != null) {
               suggestFilterTypes(contextAtCursor, importDecorator, compilationResult)
            } else {
               val typesInQuery = findTypesDeclaredInQuery(contextAtCursor, compilationResult)
               // If the user has provided facts, (either in a Given clause, or in the body of the query),
               // Then only suggest the types that are discoverable based on what they've provided.
               if (typesInQuery.isNotEmpty()) {
                  findModelsReturnsFromProvidedInputs(
                     importDecorator,
                     typesInQuery,
                     includeModelAttributes = true
                  )
               } else {
                  val queryMode = findQueryMode(contextAtCursor)
                  findModelsReturnableFromNoArgServices(importDecorator, queryMode) +
                     findModelsReturnableFromQueryOperations(importDecorator, queryMode)
               }
            }
         }
         is VariableNameContext -> suggestTypesAsInputs(importDecorator)
         // Filter operations - eg: find { Film( <-- here
         is ArrayMarkerContext -> suggestFilterTypes(contextAtCursor, importDecorator, compilationResult)
         else -> emptyList()
      }
      val distinctCompletions =
         completions.distinctBy { completion ->
            completion.additionalTextEdits.orEmpty().map { edit -> edit.newText } + completion.insertText
         }
      return completed(distinctCompletions)
   }

   private fun buildAsCompletion(params: CompletionParams, sourceTypeIsCollection: Boolean): List<CompletionItem> {
      return listOf(CompletionItem("{ ... } (Start a projection)").apply {
         kind = CompletionItemKind.Snippet
         insertTextFormat = InsertTextFormat.Snippet
         insertText = "{\n\t$0\n}"
         if (sourceTypeIsCollection) {
            insertText += "[]"
         }
      })
   }

   private fun suggestFilterTypes(
      contextAtCursor: ParserRuleContext,
      importDecorator: ImportCompletionDecorator,
      compilationResult: CompilationResult
   ): List<CompletionItem> {
      val typeToFilterToken = contextAtCursor.searchUpForRule<OptionalTypeReferenceContext>()
         ?: // Hmm... this shouldn't happen.
         return emptyList()
      val typeReferenceToken = typeToFilterToken.typeReference()
      val typeToFilter = compilationResult.compiler.lookupTypeByName(typeReferenceToken)
         .toVyneQualifiedName()
      val isExposedByQueryOperations = schema.queryOperations
         .any { it.returnTypeName == typeToFilter }
      val queryOperationAttributes = if (isExposedByQueryOperations) {
         schema.type(typeToFilter).attributes.map { (fieldName, field) ->
            typeProvider.buildCompletionItem(null, field.type.toTaxiQualifiedName(), listOf(importDecorator))
         }
      } else {
         emptyList()
      }
      // Find any operations that return the type we're looking for.
      // Add their parameters as completion items
      val operationsReturningType = schema.operations
         .filter { it.returnTypeName == typeToFilter }
         .flatMap { operation ->
            operation.parameters.map { param ->
               typeProvider.buildCompletionItem(param.type.taxiType, listOf(importDecorator))
            }
         }
      return queryOperationAttributes + operationsReturningType
   }

   private fun findQueryMode(contextAtCursor: ParserRuleContext): QueryMode? {
      val queryDirective =
         contextAtCursor.searchUpForRule(listOf(QueryDirectiveContext::class.java)) as QueryDirectiveContext?
      // Try to pick the query mode (might be invalid, because the user is still typing)
      val queryMode = queryDirective?.let {
         try {
            QueryMode.forToken(it.text)
         } catch (e: Exception) {
            null
         }
      }
      return queryMode
   }

   private fun findModelsReturnsFromProvidedInputs(
      importDecorator: ImportCompletionDecorator,
      typesInQuery: List<QualifiedName>,
      includeModelAttributes: Boolean = false
   ): List<CompletionItem> {
      val discoverableTypes = typesInQuery.flatMap { typeInQuery ->
         Algorithms.findAccessibleModels(schema, typeInQuery.parameterizedName.fqn())
      }.flatMap { schemaSearchResult ->
         val discoveryPath = schemaSearchResult.describePath()
         val modelCompletionItem = completionItemWithDiscoveryPath(
            schemaSearchResult.resultingType.toTaxiQualifiedName(),
            discoveryPath,
            importDecorator
         )
         val attributeCompletionItems = if (includeModelAttributes) {
            schema.type(schemaSearchResult.resultingType).attributes
               // Don't include primitive attributes, since they're not really queryable
               .filter { (_, field) -> !schema.type(field.type).isPrimitive }
               .map { (fieldName, field) ->
                  completionItemWithDiscoveryPath(
                     field.type.toTaxiQualifiedName(),
                     discoveryPath + fieldName,
                     importDecorator
                  )
               }
         } else emptyList()
         listOf(modelCompletionItem) + attributeCompletionItems
      }
      val attributesOfTypesInQuery = typesInQuery.flatMap { typeInQuery ->
         if (includeModelAttributes) {
            schema.type(typeInQuery.toVyneQualifiedName()).attributes
               // Don't include primitive attributes, since they're not really queryable
               .filter { (_, field) -> !schema.type(field.type).isPrimitive }
               .map { (fieldName, field) ->
                  completionItemWithDiscoveryPath(
                     typeName = field.type.toTaxiQualifiedName(),
                     discoveryPath = listOf(typeInQuery.toVyneQualifiedName().shortDisplayName, fieldName),
                     importDecorator = importDecorator
                  )
               }
         } else {
            emptyList()
         }
      }
      val completionItems = attributesOfTypesInQuery + discoverableTypes
      return completionItems.distinctBy { it.label }
   }

   private fun findTypesDeclaredInQuery(
      contextAtCursor: ParserRuleContext,
      compilationResult: CompilationResult
   ): List<QualifiedName> {
      val queryTypes = contextAtCursor.searchUpForRule(listOf(QueryTypeListContext::class.java))?.let { it ->
         val queryTypeList = it as QueryTypeListContext
         queryTypeList.fieldTypeDeclaration()
            .map { fieldTypeContext -> compilationResult.compiler.lookupTypeByName(fieldTypeContext.optionalTypeReference().typeReference()) }
      } ?: emptyList()
      val factTypes = findTypesInGivenClause(contextAtCursor, compilationResult)
      return canonicalizeTypeNames(queryTypes + factTypes)
   }

   /**
    * Take a list of QualifiedNames which may not be full type names, and resovles
    * them against the schema.
    *
    * Where type names are incomplete, but unambiguous, the returned qualifiedName is the fully
    * resolved qualified name
    *
    * If a type is not present in the schema, or cannot be unabmiguously resolved, it is not returned
    */
   private fun canonicalizeTypeNames(qualifiedNames: List<QualifiedName>): List<QualifiedName> {
      return qualifiedNames.filter {
         schema.hasType(it.parameterizedName)
      }.map { schema.type(it.parameterizedName).name.toTaxiQualifiedName() }
   }

   private fun findTypesInGivenClause(
      contextAtCursor: ParserRuleContext,
      compilationResult: CompilationResult
   ): List<QualifiedName> {
      val factTypes = contextAtCursor.searchUpForRule(listOf(GivenBlockContext::class.java))?.let { ruleContext ->
         val givenBlock = ruleContext as GivenBlockContext
         val typesOfFacts = givenBlock.getRuleContexts(FactListContext::class.java)
            .flatMap { it.fact() }
            .map { it.getRuleContexts(TypeReferenceContext::class.java) }
            .flatten()
            .map { typeTypeContext ->
               compilationResult.compiler.lookupTypeByName(typeTypeContext)
            }
         typesOfFacts
      } ?: emptyList()
      return factTypes
   }

   private fun completionItemWithDiscoveryPath(
      typeName: QualifiedName,
      discoveryPath: List<String>,
      importDecorator: ImportCompletionDecorator
   ) =
      typeProvider.buildCompletionItem(
         null, typeName, listOf(
            importDecorator,
            documentationDecorator(
               "Discovered by path ${discoveryPath.joinToString(" -> ")}"
            )
         )
      )

   private fun suggestTypesAsInputs(importDecorator: ImportCompletionDecorator): List<CompletionItem> {
      return typeProvider.getTypes(listOf(importDecorator))
   }

   private fun findModelsReturnableFromQueryOperations(
      importDecorator: ImportCompletionDecorator,
      queryMode: QueryMode?
   ): List<CompletionItem> {
      return schema.queryOperations
         .filter { operation -> !operation.returnType.isPrimitive }
         .filter { operation -> filterStreamingOperations(queryMode, operation) }
         .map { queryOperation ->
            val (serviceName, operationName) = OperationNames.serviceAndOperation(queryOperation.qualifiedName)
            val service = schema.service(serviceName)
            operationReturnTypeCompletionItem(queryOperation, importDecorator, service)
         }
   }

   private fun filterStreamingOperations(
      queryMode: QueryMode?,
      operation: RemoteOperation
   ) = when (queryMode) {
      QueryMode.FIND_ALL,
      QueryMode.FIND_ONE -> !operation.returnType.isStream
      QueryMode.STREAM -> operation.returnType.isStream
      else -> true
   }

   private fun findModelsReturnableFromNoArgServices(
      importDecorator: ImportCompletionDecorator,
      queryMode: QueryMode?
   ): List<CompletionItem> {
      val types = schema.operationsWithNoArgument()
         .filter { (_, operation) -> !operation.returnType.isPrimitive }
         .filter { (_, operation) -> filterStreamingOperations(queryMode, operation) }
         .map { (service, operation) ->
            operationReturnTypeCompletionItem(operation, importDecorator, service)
         }
      return types
   }

   private fun operationReturnTypeCompletionItem(
      operation: RemoteOperation,
      importDecorator: ImportCompletionDecorator,
      service: Service
   ) = typeProvider.buildCompletionItem(
      schema.taxiType(operation.returnType.qualifiedName),
      listOf(
         importDecorator,
         documentationDecorator("Returned from ${service.name.shortDisplayName} -> ${operation.qualifiedName.shortDisplayName}")
      )
   )

   private fun documentationDecorator(documentation: String): CompletionDecorator {
      return object : CompletionDecorator {
         override fun decorate(typeName: QualifiedName, type: Type?, completionItem: CompletionItem): CompletionItem {
            completionItem.setDocumentation(MarkupContent("markdown", documentation))
            return completionItem
         }

      }
   }


   private fun searchUpForHighestRuleEndingBefore(
      position: Position,
      context: ParserRuleContext,
      lastChildChecked: ParserRuleContext? = null
   ): ParserRuleContext? {
      // Antlr line numbers are one-based. :(
      val oneBasedLineNumber = position.line + 1
      if (context.parent == null) {
         return lastChildChecked
      }
      val parentContext = context.parent as? ParserRuleContext? ?: return lastChildChecked
      val parentStop = parentContext.stop
      if (parentStop.locationIsBeforeOrEqualTo(
            oneBasedLineNumber,
            position.character
         ) && parentStop.locationIsAfterOrEqualTo(context.stop.line, context.stop.charPositionInLine)
      ) {
         searchUpForHighestRuleEndingBefore(position, parentContext, context)
      }

      when (context) {
      }
      if (context.stop.line < oneBasedLineNumber) {
         return null
      }
      TODO()
   }

}

private fun Token.locationIsBeforeOrEqualTo(oneBasedLineNumber: Int, character: Int): Boolean {
   return this.line <= oneBasedLineNumber && this.charPositionInLine <= character
}

private fun Token.locationIsAfterOrEqualTo(oneBasedLineNumber: Int, character: Int): Boolean {
   return when {
      this.line < oneBasedLineNumber -> false
      this.line >= oneBasedLineNumber && this.charPositionInLine >= character -> true
      else -> false
   }
}

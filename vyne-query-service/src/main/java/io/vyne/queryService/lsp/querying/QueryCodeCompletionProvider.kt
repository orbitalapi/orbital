package io.vyne.queryService.lsp.querying

import io.vyne.query.graph.Algorithms
import io.vyne.schemas.*
import lang.taxi.TaxiParser.ClassOrInterfaceTypeContext
import lang.taxi.TaxiParser.ConditionalTypeStructureDeclarationContext
import lang.taxi.TaxiParser.FactListContext
import lang.taxi.TaxiParser.FieldDeclarationContext
import lang.taxi.TaxiParser.GivenBlockContext
import lang.taxi.TaxiParser.ListTypeContext
import lang.taxi.TaxiParser.QueryDirectiveContext
import lang.taxi.TaxiParser.QueryTypeListContext
import lang.taxi.TaxiParser.SingleNamespaceDocumentContext
import lang.taxi.TaxiParser.TemporalFormatListContext
import lang.taxi.TaxiParser.TypeBodyContext
import lang.taxi.TaxiParser.TypeTypeContext
import lang.taxi.TaxiParser.VariableNameContext
import lang.taxi.lsp.CompilationResult
import lang.taxi.lsp.completion.*
import lang.taxi.searchUpForRule
import lang.taxi.types.QualifiedName
import lang.taxi.types.QueryMode
import lang.taxi.types.Type
import org.antlr.v4.runtime.ParserRuleContext
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.MarkupContent
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
      contextAtCursor: ParserRuleContext?
   ): CompletableFuture<List<CompletionItem>> {
      if (contextAtCursor == null) {
         return completed(topLevelQueryCompletionItems)
      }
      val completions = when (contextAtCursor) {
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
         is FieldDeclarationContext,
         is TypeBodyContext,
         is ClassOrInterfaceTypeContext -> {
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
         is ListTypeContext,
            // The grammar may parse an open parenthesis as a Temproal format
         is TemporalFormatListContext -> suggestFilterTypes(contextAtCursor, importDecorator, compilationResult)
         else -> emptyList()
      }
      val distinctCompletions =
         completions.distinctBy { completion ->
            completion.additionalTextEdits.orEmpty().map { edit -> edit.newText } + completion.insertText
         }
      return completed(distinctCompletions)
   }

   private fun suggestFilterTypes(
      contextAtCursor: ParserRuleContext,
      importDecorator: ImportCompletionDecorator,
      compilationResult: CompilationResult
   ): List<CompletionItem> {
      val typeToFilterToken = contextAtCursor.searchUpForRule(TypeTypeContext::class.java)
         ?: // Hmm... this shouldn't happen.
         return emptyList()
      val typeToFilter = compilationResult.compiler.lookupTypeByName(typeToFilterToken as TypeTypeContext)
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
         queryTypeList.typeType()
            .map { typeTypeContext -> compilationResult.compiler.lookupTypeByName(typeTypeContext) }
      } ?: emptyList()
      val factTypes = findTypesInGivenClause(contextAtCursor, compilationResult)
      return (queryTypes + factTypes).filter {
         // It's possible (because the user is typing) that not everything listed is an actual type.
         // Filter down to just the stuff that counts
         schema.hasType(it.parameterizedName)
      }
   }

   private fun findTypesInGivenClause(
      contextAtCursor: ParserRuleContext,
      compilationResult: CompilationResult
   ): List<QualifiedName> {
      val factTypes = contextAtCursor.searchUpForRule(listOf(GivenBlockContext::class.java))?.let { ruleContext ->
         val givenBlock = ruleContext as GivenBlockContext
         val typesOfFacts = givenBlock.getRuleContexts(FactListContext::class.java)
            .flatMap { it.fact() }
            .map { it.getRuleContexts(TypeTypeContext::class.java) }
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


}

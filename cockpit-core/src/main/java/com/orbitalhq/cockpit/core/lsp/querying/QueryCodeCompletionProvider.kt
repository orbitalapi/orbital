package com.orbitalhq.cockpit.core.lsp.querying

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.right
import com.orbitalhq.query.graph.Algorithms
import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.toTaxiQualifiedName
import com.orbitalhq.schemas.toVyneQualifiedName
import lang.taxi.CompilationError
import lang.taxi.TaxiParser.ArrayMarkerContext
import lang.taxi.TaxiParser.ExpressionGroupContext
import lang.taxi.TaxiParser.FactDeclarationContext
import lang.taxi.TaxiParser.FactListContext
import lang.taxi.TaxiParser.FunctionCallContext
import lang.taxi.TaxiParser.GivenBlockContext
import lang.taxi.TaxiParser.IdentifierContext
import lang.taxi.TaxiParser.MemberReferenceContext
import lang.taxi.TaxiParser.MutationContext
import lang.taxi.TaxiParser.NullableTypeReferenceContext
import lang.taxi.TaxiParser.ParameterConstraintContext
import lang.taxi.TaxiParser.QualifiedNameContext
import lang.taxi.TaxiParser.QueryDirectiveContext
import lang.taxi.TaxiParser.QueryOrMutationContext
import lang.taxi.TaxiParser.SingleNamespaceDocumentContext
import lang.taxi.TaxiParser.ToplevelObjectContext
import lang.taxi.TaxiParser.TypeReferenceContext
import lang.taxi.TaxiParser.VariableNameContext
import lang.taxi.expressions.ServiceExpression
import lang.taxi.lsp.CompilationResult
import lang.taxi.lsp.completion.CompletionDecorator
import lang.taxi.lsp.completion.CompletionItemList
import lang.taxi.lsp.completion.CompletionProvider
import lang.taxi.lsp.completion.EditorCompletionService
import lang.taxi.lsp.completion.ImportCompletionDecorator
import lang.taxi.lsp.completion.TypeCompletionBuilder
import lang.taxi.lsp.completion.TypeRepository
import lang.taxi.lsp.completion.asCompletionItemList
import lang.taxi.lsp.completion.asExclusiveCompletionItemList
import lang.taxi.lsp.completion.completed
import lang.taxi.lsp.completion.locationIsAfterOrEqualTo
import lang.taxi.lsp.completion.locationIsBeforeOrEqualTo
import lang.taxi.query.QueryMode
import lang.taxi.searchUpForRule
import lang.taxi.types.Named
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type
import mu.KotlinLogging
import org.antlr.v4.runtime.ParserRuleContext
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.InsertTextFormat
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.Position
import java.util.concurrent.CompletableFuture

/**
 * Completion provider focussed on delivering hints when writing queries
 */
class QueryCodeCompletionProvider(
   private val typeCompletionBuilder: TypeCompletionBuilder,
   private val schema: Schema
) :
   CompletionProvider {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   // The "normal" editor, not used when building queries
   private val editorCompletionService = EditorCompletionService(typeCompletionBuilder)

   private val topLevelQueryCompletionItems:CompletionItemList = listOf(
      "find" to "Query for a single item, or a list of items",
      "stream" to "Query for a continuous stream of data",
      "given" to "Provide a set of facts for the starting point for a query",
      "call" to "Invoke a mutating service",
   )
      .map { (text, description) ->
         CompletionItem(text).apply {
            kind = CompletionItemKind.Keyword
            insertText = text
            detail = description
            additionalTextEdits = emptyList()
         }
      }.asCompletionItemList()

   /**
    * Looking for scenario like:
    *find { Studio( <-- cursor is here )}
    */
   private fun isDefiningConstraint(contextAtCursor: IdentifierContext): Boolean {
      return contextAtCursor.parent?.parent is FunctionCallContext // Studio( )
         && contextAtCursor.searchUpForRule<QueryOrMutationContext>() != null // find { Studio ( ) }
   }

   override fun getCompletionsForContext(
      compilationResult: CompilationResult,
      params: CompletionParams,
      importDecorator: ImportCompletionDecorator,
      contextAtCursor: ParserRuleContext?,
      lastSuccessfulCompilation: CompilationResult?,
      typeRepository: TypeRepository
   ): CompletableFuture<CompletionItemList> {
      // The typeRepository passed to us is based off the compilation result.
      // However, that is often inaccurate, as the user is editing a query, so fails compilation
      // and therefore is incomplete.
      val schemaTypeRepository = SchemaTypeRepository(schema)

      if (contextAtCursor == null) {
         return completed(topLevelQueryCompletionItems)
      }

      val significantContext = editorCompletionService.getSignificantContext(contextAtCursor)
      if (significantContext != contextAtCursor) {
         return getCompletionsForContext(
            compilationResult,
            params,
            importDecorator,
            significantContext,
            lastSuccessfulCompilation
         )
      }

      val decorators = listOf(importDecorator)


      val completions:CompletionItemList = when {
         contextAtCursor is IdentifierContext && isDefiningConstraint(contextAtCursor) -> {
            suggestFilterTypes(contextAtCursor, importDecorator, compilationResult)
         }
//         contextAtCursor is TypeProjectionContext -> {
//            if (contextAtCursor.stop.text == "as") {
//                              val queryTypeListContext =
//                                 contextAtCursor.searchUpForRule(QueryTypeListContext::class.java) as? ParserRuleContext?
//                              val children = queryTypeListContext?.children ?: emptyList()
//                              // If the source type is a collection...
//                              val sourceTypeIsCollection =
//                                 children.isNotEmpty() && children.filterIsInstance<FieldTypeDeclarationContext>().any {
//                                    it.nullableTypeReference()?.typeReference()?.arrayMarker() != null
//                                 }
//                              if (children.isNotEmpty()) {
//                                 buildAsCompletion(params, sourceTypeIsCollection)
//                              } else {
//                                 emptyList()
//                              }
//            } else {
//               emptyList()
//            }
//         }
         contextAtCursor is SingleNamespaceDocumentContext -> topLevelQueryCompletionItems
         contextAtCursor is QueryDirectiveContext -> {
            // This is a find { ... }, possibly with a given { .. }.
            // It's not a projection (yet).
            val typesInGivenClause = findTypesInGivenClause(contextAtCursor, compilationResult)
            if (typesInGivenClause.isNotEmpty()) {
               findModelsReturnsFromProvidedInputs(
                  importDecorator,
                  typesInGivenClause,
                  includeModelAttributes = typesInGivenClause.isNotEmpty() // exclude model attributes if we're just in a find { .. }
               ).asCompletionItemList()
            } else {
               val queryMode = findQueryMode(contextAtCursor)
               findModelsReturnableFromNoArgServices(importDecorator, queryMode).asCompletionItemList() +
                  findModelsReturnableFromQueryOperations(importDecorator, queryMode).asCompletionItemList()
            }
         }

         // Mutations
         contextAtCursor is TypeReferenceContext &&  isDefiningMemberReference(contextAtCursor) -> suggestCallMutationTargets(
            contextAtCursor, decorators, compilationResult, lastSuccessfulCompilation
         )
         contextAtCursor is QualifiedNameContext && isDefiningMemberReference(contextAtCursor) -> suggestCallMutationTargets(
            contextAtCursor, decorators, compilationResult, lastSuccessfulCompilation
         )
         contextAtCursor is IdentifierContext && isDefiningMemberReference(contextAtCursor) -> suggestCallMutationTargets(
            contextAtCursor, decorators, compilationResult, lastSuccessfulCompilation
         )
         contextAtCursor is IdentifierContext && isDefiningMutation(contextAtCursor) -> suggestCallMutationTargets(
            contextAtCursor, decorators, compilationResult, lastSuccessfulCompilation
         )
         contextAtCursor is MutationContext -> {
            suggestCallMutationTargets(contextAtCursor, decorators, compilationResult, lastSuccessfulCompilation)
         }

         contextAtCursor is ParameterConstraintContext -> {
            suggestFilterTypes(contextAtCursor, importDecorator, compilationResult)
         }

         contextAtCursor is VariableNameContext -> suggestTypesAsInputs(schemaTypeRepository, importDecorator)
         // Filter operations - eg: find { Film( <-- here
         contextAtCursor is ArrayMarkerContext -> suggestFilterTypes(
            contextAtCursor,
            importDecorator,
            compilationResult
         )

         else -> CompletionItemList.empty()
      }

      return completed(completions)
   }

   /**
    * Indicates if the user is typing A::B
    */
   private fun isDefiningMemberReference(contextAtCursor: ParserRuleContext): Boolean {
      return contextAtCursor.searchUpForRule<MemberReferenceContext>() != null
   }

   private fun isDefiningMutation(contextAtCursor: ParserRuleContext): Boolean {
      if (contextAtCursor.searchUpForRule<MutationContext>() != null) return true

      // because of grammar changes that allowed top-level expressions,
      // a call statement appears as a expression until the user adds a call target
      // Therefore, look for an identifier with the text of "call", that is the child of a expression
      // group sitting directly under the top level object
      return contextAtCursor is IdentifierContext && contextAtCursor.text == "call" &&
         contextAtCursor.searchUpForRule<ExpressionGroupContext>() != null
         && contextAtCursor.searchUpForRule<ExpressionGroupContext>()!!.parent is ToplevelObjectContext
   }

   private enum class SuggestServiceOrMember {
      Service,
      Member
   }

   private fun suggestCallMutationTargets(
      contextAtCursor: ParserRuleContext,
      decorators: List<CompletionDecorator>,
      compilationResult: CompilationResult,
      lastSuccessfulCompilation: CompilationResult?
   ): CompletionItemList {
      val serviceOrMember = contextAtCursor.searchUpForRule<MemberReferenceContext>().let { memberReferenceContext ->
         if (memberReferenceContext == null) {
            SuggestServiceOrMember.Service
         } else {
            SuggestServiceOrMember.Member
         }
      }

      return when (serviceOrMember) {
         SuggestServiceOrMember.Service -> suggestServices(lastSuccessfulCompilation, decorators)
         SuggestServiceOrMember.Member -> suggestMembersOfService(contextAtCursor, lastSuccessfulCompilation, decorators)
      }
   }

   private fun suggestServices(
      lastSuccessfulCompilation: CompilationResult?,
      decorators: List<CompletionDecorator>
   ): CompletionItemList {
      if (lastSuccessfulCompilation == null) {
         return CompletionItemList.empty()
      }
      return lastSuccessfulCompilation.documentOrEmpty.services
         .map { service ->
            typeCompletionBuilder.buildCompletionItem(
               service,
               service.toQualifiedName(),
               decorators
            )
         }.asExclusiveCompletionItemList() // Only show these, not others
   }

   private fun suggestMembersOfService(
      contextAtCursor: ParserRuleContext,
      lastSuccessfulCompilation: CompilationResult?,
      decorators: List<CompletionDecorator>,
   ): CompletionItemList{
      if (lastSuccessfulCompilation == null) {
         return CompletionItemList.empty()
      }
      // Foo::Bar
      val serviceAndMember = contextAtCursor.searchUpForRule<MemberReferenceContext>() ?: return CompletionItemList.empty()
      // In Foo::Bar, select Foo

      val service = serviceAndMember.typeReference() ?: return CompletionItemList.empty()
      // The LHS is an expression - compile it
      // We could just try and read the text here, but that's error-prone
      val lhsExpressionContext = service.searchUpForRule<ExpressionGroupContext>()?.expressionGroup()?.firstOrNull() ?: return CompletionItemList.empty()
      val lhsExpression = lastSuccessfulCompilation.compiler.compileExpression(lhsExpressionContext).getOrNull() ?: return CompletionItemList.empty()
      val completionsOrErrors: Either<List<CompilationError>, CompletionItemList> = if (lhsExpression is ServiceExpression) {
         lhsExpression.service.members.map { member ->
            // Don't decorate members, as they're not importable on their own
            typeCompletionBuilder.buildCompletionItem(member, member.toQualifiedName(), emptyList())
         }.asExclusiveCompletionItemList().right()
      } else {
         CompletionItemList.empty().right()
      }
      return completionsOrErrors.getOrElse { CompletionItemList.empty() }
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
   ): CompletionItemList {
      val typeToFilter = findFilterTargetType(contextAtCursor, compilationResult)
         ?: return CompletionItemList.empty()
      val isExposedByQueryOperations = (schema.queryOperations + schema.tableOperations)
         .any { operation ->
            val returnType = operation.returnType.collectionType ?: operation.returnType
            returnType.name == typeToFilter
         }
      val queryOperationAttributes = if (isExposedByQueryOperations) {
         schema.type(typeToFilter).attributes.map { (fieldName, field) ->
            typeCompletionBuilder.buildCompletionItem(schema.type(field.type).taxiType, field.type.toTaxiQualifiedName(), listOf(importDecorator))
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
               typeCompletionBuilder.buildCompletionItem(param.type.taxiType, listOf(importDecorator))
            }
         }
      return (queryOperationAttributes + operationsReturningType).asExclusiveCompletionItemList()
   }

   private fun findFilterTargetType(
      contextAtCursor: ParserRuleContext,
      compilationResult: CompilationResult
   ): com.orbitalhq.schemas.QualifiedName? {
      contextAtCursor.searchUpForRule<NullableTypeReferenceContext>()?.let { typeToFilterToken ->
         val typeReferenceToken = typeToFilterToken.typeReference()
         return compilationResult.compiler.lookupTypeByName(typeReferenceToken)
            .toVyneQualifiedName()
      }
      contextAtCursor.searchUpForRule<QualifiedNameContext>()?.let { qualifiedNameContext ->
         return qualifiedNameContext.text.fqn()
      }

      // couldn't find it
      return null
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
            schema.type(schemaSearchResult.resultingType).taxiType,
            discoveryPath,
            importDecorator
         )
         val attributeCompletionItems = if (includeModelAttributes) {
            schema.type(schemaSearchResult.resultingType).attributes
               // Don't include primitive attributes, since they're not really queryable
               .filter { (_, field) -> !field.resolveType(schema).isPrimitive }
               .map { (fieldName, field) ->
                  completionItemWithDiscoveryPath(
                     schema.type(field.type).taxiType,
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
               .filter { (_, field) -> !field.resolveType(schema).isPrimitive }
               .map { (fieldName, field) ->
                  completionItemWithDiscoveryPath(
                     type = schema.taxiType(field.type),
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
            .map { factContext ->
               factContext.getRuleContexts(FactDeclarationContext::class.java)
                  .flatMap { factDeclaration -> factDeclaration.getRuleContexts(TypeReferenceContext::class.java) }

            }
            .flatten()
            .map { typeTypeContext ->
               compilationResult.compiler.lookupTypeByName(typeTypeContext)
            }
         typesOfFacts
      } ?: emptyList()
      return factTypes
   }

   private fun completionItemWithDiscoveryPath(
      type: Type,
      discoveryPath: List<String>,
      importDecorator: ImportCompletionDecorator
   ) =
      typeCompletionBuilder.buildCompletionItem(
         type, type.toQualifiedName(), listOf(
            importDecorator,
            documentationDecorator(
               "Discovered by path ${discoveryPath.joinToString(" -> ")}"
            )
         )
      )

   private fun suggestTypesAsInputs(
      typeRepository: TypeRepository,
      importDecorator: ImportCompletionDecorator
   ): CompletionItemList {
      return typeCompletionBuilder.getTypes(typeRepository, listOf(importDecorator))
         .asCompletionItemList()
   }

   private fun findModelsReturnableFromQueryOperations(
      importDecorator: ImportCompletionDecorator,
      queryMode: QueryMode?
   ): List<CompletionItem> {
      return schema.queryAndTableOperations
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
   ) = typeCompletionBuilder.buildCompletionItem(
      schema.taxiType(operation.returnType.qualifiedName),
      listOf(
         importDecorator,
         documentationDecorator("Returned from ${service.name.shortDisplayName} -> ${operation.qualifiedName.shortDisplayName}")
      )
   )

   private fun documentationDecorator(documentation: String): CompletionDecorator {
      return object : CompletionDecorator {
         override fun decorate(
            typeName: QualifiedName,
            token: Named?,
            completionItem: CompletionItem
         ): CompletionItem {
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

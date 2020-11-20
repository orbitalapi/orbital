package io.vyne.vyneql.compiler

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.vyne.VyneQLBaseListener
import io.vyne.VyneQLParser
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.schemas.Schema
import io.vyne.schemas.toVyneQualifiedName
import io.vyne.vyneql.AnonymousFieldDefinition
import io.vyne.vyneql.AnonymousTypeDefinition
import io.vyne.vyneql.ComplexFieldDefinition
import io.vyne.vyneql.DiscoveryType
import io.vyne.vyneql.ProjectedType
import io.vyne.vyneql.QueryMode
import io.vyne.vyneql.SelfReferencedFieldDefinition
import io.vyne.vyneql.SimpleAnonymousFieldDefinition
import io.vyne.vyneql.VyneQlQuery
import lang.taxi.CompilationError
import lang.taxi.CompilationException
import lang.taxi.NamespaceQualifiedTypeResolver
import lang.taxi.Namespaces
import lang.taxi.TaxiDocument
import lang.taxi.TaxiParser
import lang.taxi.Tokens
import lang.taxi.searchUpForRule
import lang.taxi.text
import lang.taxi.types.ArrayType
import lang.taxi.types.Arrays
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type
import lang.taxi.utils.flattenErrors
import lang.taxi.utils.invertEitherList
import lang.taxi.utils.wrapErrorsInList
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RuleContext

class TokenProcessor(private val taxi: TaxiDocument, private val schema: Schema) : VyneQLBaseListener() {
   private val typeResolver: NamespaceQualifiedTypeResolver = object : NamespaceQualifiedTypeResolver {
      override val namespace: String = Namespaces.DEFAULT_NAMESPACE // We don't support ns in VyneQl yet

      override fun resolve(context: TaxiParser.TypeTypeContext) = error("Not supported")

      override fun resolve(requestedTypeName: String, context: ParserRuleContext): Either<List<CompilationError>, Type> {
         return lookupTypeFromClassTypeContext(requestedTypeName, context)
            .wrapErrorsInList()
            .map { taxi.type(it) }
      }
   }
   private val constraintBuilder = ConstraintBuilder(taxi, typeResolver)

   private lateinit var parsedQuery: Either<List<CompilationError>, VyneQlQuery>

   val result: Either<List<CompilationError>, VyneQlQuery>
      get() {
         return parsedQuery
      }
   val errors: List<CompilationError>
      get() {
         return when (val queryResult = parsedQuery) {
            is Either.Left -> queryResult.a
            is Either.Right -> emptyList()
         }
      }

   val query: VyneQlQuery
      get() {
         return when (val queryResult = parsedQuery) {
            is Either.Left -> throw CompilationException(errors) // That'll teach you for calling...
            is Either.Right -> queryResult.b
         }
      }

   override fun exitImportDeclaration(ctx: VyneQLParser.ImportDeclarationContext) {
      super.exitImportDeclaration(ctx)
   }

   override fun exitQueryName(ctx: VyneQLParser.QueryNameContext) {
      super.exitQueryName(ctx)
   }

   override fun exitNamedQuery(ctx: VyneQLParser.NamedQueryContext) {
      val queryName = ctx.queryName().Identifier().text
      val parametersOrErrors = ctx.queryName().queryParameters()?.queryParamList()?.queryParam()?.map { queryParam ->
         val parameterName = queryParam.Identifier().text
         val queryParameter: Either<CompilationError, Pair<String, QualifiedName>> = lookupType(queryParam.typeType()).map { parameterType ->
            parameterName to parameterType
         }
         queryParameter
      }?.invertEitherList() ?: Either.right(emptyList())


      this.parsedQuery = parametersOrErrors.flatMap { parameters ->
         parseQueryBody(queryName, parameters.toMap(), ctx.queryBody())
      }
   }

   override fun exitAnonymousQuery(ctx: VyneQLParser.AnonymousQueryContext) {
      this.parsedQuery = parseQueryBody("", emptyMap(), ctx.queryBody())
         .map { query ->
            val derivedName = query.queryMode.directive +
               query.typesToFind.joinToString("And") { it.type.typeName }
            query.copy(name = derivedName)
         }
   }

   private fun parseQueryBody(name: String, parameters: Map<String, QualifiedName>, ctx: VyneQLParser.QueryBodyContext): Either<List<CompilationError>, VyneQlQuery> {
      val queryDirective = when {
         ctx.queryDirective().findAllDirective() != null -> QueryMode.FIND_ALL
         ctx.queryDirective().findOneDirective() != null -> QueryMode.FIND_ONE
         else -> error("Unhandled Query Directive")
      }

      val factsOrErrors = ctx.givenBlock()?.let { parseFacts(it) } ?: Either.right(emptyMap())
      val queryOrErrors = factsOrErrors.flatMap { facts ->
         parseQueryTypeList(ctx.queryTypeList())
            .flatMap { typesToDiscover ->

               parseTypeToProject(ctx.queryProjection(), typesToDiscover)
                  .wrapErrorsInList()
                  .map { typeToProject ->
                     VyneQlQuery(
                        name = name,
                        facts = facts,
                        queryMode = queryDirective,
                        parameters = parameters,
                        typesToFind = typesToDiscover,
                        projectedType = typeToProject
                     )
                  }
            }
      }
      return queryOrErrors
   }

   private fun parseFacts(givenBlock: VyneQLParser.GivenBlockContext): Either<List<CompilationError>, Map<String, TypedInstance>> {
      return givenBlock.factList().fact().map {
         parseFact(it)
      }.invertEitherList()
         .map { it.toMap() }
   }

   private fun parseFact(factCtx: VyneQLParser.FactContext): Either<CompilationError, Pair<String, TypedInstance>> {
      val variableName = factCtx.variableName().Identifier().text
      return lookupType(factCtx.typeType()).flatMap { factType ->
         try {
            Either.right(variableName to TypedInstance.from(schema.type(factType.toVyneQualifiedName()), factCtx.literal().value(), schema, source = Provided))
         } catch (e: Exception) {
            Either.left(CompilationError(factCtx.start, "Failed to create TypedInstance - ${e.message}"))
         }

      }

   }

   private fun parseTypeToProject(queryProjection: VyneQLParser.QueryProjectionContext?,
                                  typesToDiscover: List<DiscoveryType>): Either<CompilationError, ProjectedType?> {
      if (queryProjection == null) {
         return Either.right(null)
      }

      val projectionType = queryProjection.typeType()
      val anonymousProjectionType = queryProjection.anonymousTypeDefinition()


      if (anonymousProjectionType != null && projectionType == null && typesToDiscover.size > 1) {
         return Either.left(CompilationError(queryProjection.start,
            "When anonymous projected type is defined without an explicit based discoverable type sizes should be 1"))
      }

      if (projectionType != null && projectionType.listType() == null && anonymousProjectionType == null && typesToDiscover.size == 1 && typesToDiscover.first().type.parameters.isNotEmpty()) {
         return Either.left(CompilationError(queryProjection.start,
            "projection type is a list but the type to discover is not, both should either be list or single entity."))
      }

      if (anonymousProjectionType != null && anonymousProjectionType.listType() == null && typesToDiscover.size == 1 && typesToDiscover.first().type.parameters.isNotEmpty()) {
         return Either.left(CompilationError(queryProjection.start,
            "projection type is a list but the type to discover is not, both should either be list or single entity."))
      }

      return when {
         projectionType == null && anonymousProjectionType != null -> {
            val compilationErrorOrFields = anonymousProjectionType.anonymousField().map { toAnonymousFieldDefinition(it, queryProjection, typesToDiscover) }
            val fieldDefinitions = mutableListOf<AnonymousFieldDefinition>()
            compilationErrorOrFields.forEach { compilationErrorOrField ->
               when (compilationErrorOrField) {
                  is Either.Left -> {
                     return compilationErrorOrField.a.left()
                  }
                  is Either.Right -> fieldDefinitions.add(compilationErrorOrField.b)
               }
            }
            return ProjectedType.fomAnonymousTypeOnly(AnonymousTypeDefinition(anonymousProjectionType.listType() != null, fieldDefinitions.toList())).right()
         }

         projectionType != null && anonymousProjectionType == null -> {
            lookupType(projectionType).map { qualifiedName -> ProjectedType.fromConcreteTypeOnly(qualifiedName) }

         }

         projectionType != null && anonymousProjectionType != null -> {
            val concreteProjectionType = lookupType(projectionType)
            val compilationErrorOrFields = anonymousProjectionType.anonymousField().map { toAnonymousFieldDefinition(it, queryProjection, typesToDiscover) }
            val fieldDefinitions = mutableListOf<AnonymousFieldDefinition>()
            compilationErrorOrFields.forEach { compilationErrorOrField ->
               when (compilationErrorOrField) {
                  is Either.Left -> {
                     return compilationErrorOrField.a.left()
                  }
                  is Either.Right -> fieldDefinitions.add(compilationErrorOrField.b)
               }
            }

            if (concreteProjectionType is Either.Left) {
               return concreteProjectionType.a.left()
            }

            return concreteProjectionType.map {
               ProjectedType(it,
                  AnonymousTypeDefinition(anonymousProjectionType.listType() != null, fieldDefinitions.toList()))
            }
         }

         else -> Either.left(CompilationError(queryProjection.start,
            "Unexpected as definition"))
      }
   }

   private fun toAnonymousFieldDefinition(anonymousFieldContext: VyneQLParser.AnonymousFieldContext,
                                          queryProjection: VyneQLParser.QueryProjectionContext,
                                          typesToDiscover: List<DiscoveryType>):
      Either<CompilationError, AnonymousFieldDefinition> {

      return when {
         anonymousFieldContext.Identifier() != null -> fromAnonymousFieldContext(anonymousFieldContext, typesToDiscover)
         anonymousFieldContext.anonymousFieldDeclaration() != null -> {
            val fieldDeclaration = anonymousFieldContext.anonymousFieldDeclaration()
            lookupType(fieldDeclaration.typeType()).map {
               SimpleAnonymousFieldDefinition(fieldName = fieldDeclaration.Identifier().text, fieldType = it)
            }
         }
         anonymousFieldContext.anonymousFieldDeclarationWithSelfReference() != null ->
            fromAnonymousFieldDeclarationWithSelfReference(anonymousFieldContext, queryProjection, typesToDiscover)

         anonymousFieldContext.anonymousComplexFieldDeclaration() != null ->
            fromAnonymousComplexFieldDeclaration(anonymousFieldContext, queryProjection, typesToDiscover)

         else -> Either.left(CompilationError(anonymousFieldContext.start,
            "Unexpected as definition"))
      }
   }

   private fun fromAnonymousComplexFieldDeclaration(
      anonymousFieldContext: VyneQLParser.AnonymousFieldContext,
      queryProjection: VyneQLParser.QueryProjectionContext,
      typesToDiscover: List<DiscoveryType>): Either<CompilationError, AnonymousFieldDefinition> {
      //    salesPerson {
      //        firstName : FirstName
      //        lastName : LastName
      //    }(from this.salesUtCode)
      val complexFieldDefinition = anonymousFieldContext.anonymousComplexFieldDeclaration()
      val fieldName = complexFieldDefinition.Identifier().first() // e.g. 'salesPerson'
      val typeFieldReference = complexFieldDefinition.Identifier().last() // e.g. 'salesUtCode'
      val referenceFieldContainingTypeName = if (queryProjection.typeType() == null) {
         val firstTypeToDiscover = typesToDiscover.first().type
         if (firstTypeToDiscover.parameters.isEmpty()) firstTypeToDiscover.fullyQualifiedName else firstTypeToDiscover.parameters.first().fullyQualifiedName
      } else {
         queryProjection.typeType().text
      }

      return when (val sourceType = fromTypeFromClassTypeContext(referenceFieldContainingTypeName, anonymousFieldContext)) {
         is Either.Left -> sourceType.a.left()
         is Either.Right -> {
            val subFieldDefinitionOrErrors = complexFieldDefinition
               .Identifier()
               .subList(1, complexFieldDefinition.Identifier().lastIndex)
               .mapIndexed { index, identifierNode ->
                  val typeType = complexFieldDefinition.typeType(index)
                  lookupType(typeType).map {
                     SimpleAnonymousFieldDefinition(fieldName = identifierNode.text, fieldType = it)
                  }
               }

            val simpleAnonymousTypeDefinitions = mutableListOf<SimpleAnonymousFieldDefinition>()
            subFieldDefinitionOrErrors.forEach { compilationErrorOrField ->
               when (compilationErrorOrField) {
                  is Either.Left -> {
                     return compilationErrorOrField.a.left()
                  }
                  is Either.Right -> simpleAnonymousTypeDefinitions.add(compilationErrorOrField.b)
               }
            }
            ComplexFieldDefinition(fieldName.text, typeFieldReference.text, sourceType.b.toQualifiedName(), simpleAnonymousTypeDefinitions).right()
         }
      }
   }

   private fun fromAnonymousFieldDeclarationWithSelfReference(
      anonymousFieldContext: VyneQLParser.AnonymousFieldContext,
      queryProjection: VyneQLParser.QueryProjectionContext,
      typesToDiscover: List<DiscoveryType>): Either<CompilationError, AnonymousFieldDefinition> {
      // traderEmail : EmailAddress(from this.traderUtCode)
      val fieldDeclarationWithSelfReference = anonymousFieldContext.anonymousFieldDeclarationWithSelfReference()
      val fieldName = fieldDeclarationWithSelfReference.Identifier().first()
      val fieldType = fieldDeclarationWithSelfReference.typeType()
      val typeFieldReference = fieldDeclarationWithSelfReference.Identifier(1)
      val referenceFieldContainingTypeName = if (queryProjection.typeType() == null) {
         val firstTypeToDiscover = typesToDiscover.first().type
         if (firstTypeToDiscover.parameters.isEmpty()) firstTypeToDiscover.fullyQualifiedName else firstTypeToDiscover.parameters.first().fullyQualifiedName
      } else {
         queryProjection.typeType().text
      }

      return when (val sourceType = fromTypeFromClassTypeContext(referenceFieldContainingTypeName, anonymousFieldContext)) {
         is Either.Left -> sourceType.a.left()
         is Either.Right -> {
            lookupType(fieldType).map { fieldTypeQualifiedName ->
               SelfReferencedFieldDefinition(fieldName.text, fieldTypeQualifiedName, typeFieldReference.text, sourceType.b.toQualifiedName())
            }
         }
      }
   }

   private fun fromAnonymousFieldContext(anonymousFieldContext: VyneQLParser.AnonymousFieldContext, typesToDiscover: List<DiscoveryType>): Either<CompilationError, AnonymousFieldDefinition> {
      val fieldName = anonymousFieldContext.Identifier().text
      val firstType = typesToDiscover.first().type
      val firstTypeName = if (firstType.parameters.isEmpty()) firstType.fullyQualifiedName else firstType.parameters.first().fullyQualifiedName
      return when (val sourceType = fromTypeFromClassTypeContext(firstTypeName, anonymousFieldContext)) {
         is Either.Left -> sourceType.a.left()
         is Either.Right -> {
            val objectType = sourceType.b as ObjectType
            if (objectType.hasField(fieldName)) {
               SimpleAnonymousFieldDefinition(fieldName = fieldName, fieldType = objectType.field(fieldName).type.toQualifiedName()).right()
            } else {
               CompilationError(anonymousFieldContext.start,
                  "$fieldName is not part of ${objectType.toQualifiedName()}").left()
            }
         }
      }
   }


   private fun parseQueryTypeList(queryTypeList: VyneQLParser.QueryTypeListContext): Either<List<CompilationError>, List<DiscoveryType>> {
      return queryTypeList.typeType().map { queryType ->
         lookupType(queryType)
            .mapLeft { listOf(it) }
            .flatMap { qualifiedName ->
               val type = taxi.type(qualifiedName.parameterizedName)
               val constraintsOrErrors = queryType.parameterConstraint()?.parameterConstraintExpressionList()?.let { constraintExpressionList ->
                  constraintBuilder.build(constraintExpressionList, type)
               } ?: Either.right(emptyList())
               constraintsOrErrors.map { constraints ->
                  DiscoveryType(qualifiedName, constraints)
               }
            }
      }.invertEitherList().flattenErrors()
   }

   private fun lookupType(typeType: VyneQLParser.TypeTypeContext): Either<CompilationError, QualifiedName> {
      val typeName: Either<CompilationError, QualifiedName> = when {
         typeType.classOrInterfaceType() != null -> lookupTypeFromClassTypeContext(typeType.classOrInterfaceType().text, typeType)
         else -> Either.left(CompilationError(typeType.start, "This scenario is not supported.  This is likely a bug in the VyneQL compiler, and should be reported"))
      }.map { qualifiedName ->
         if (typeType.listType() != null) {
            Arrays.nameOfArray(qualifiedName)
         } else {
            qualifiedName
         }
      }
      return typeName
   }

   private fun lookupTypeFromClassTypeContext(requestedTypeName: String, context: ParserRuleContext): Either<CompilationError, QualifiedName> {
      // Handle the name doesn't need qualifying
      when {
         PrimitiveType.isPrimitiveType(requestedTypeName) -> return PrimitiveType.fromDeclaration(requestedTypeName).toQualifiedName().right()
         ArrayType.isArrayTypeName(requestedTypeName) -> return buildArrayType(context)
      }
      return if (taxi.containsType(requestedTypeName)) {
         Either.right(QualifiedName.from(requestedTypeName))
      } else {
         // Is there an explicit import?
         val imports = context.importsInFile().filter { it.typeName == requestedTypeName }
         when {
            imports.size == 1 -> Either.right(imports.first())
            imports.size > 1 -> Either.left(CompilationError(context.start, "Type $requestedTypeName is ambiguous and could refer to any of ${imports.joinToString(",")}}"))
            else -> {
               // There were no matching imports.
               // Check to see if this type is resolvable by name only
               val matchesOnNameOnly = taxi.types.filter { it.toQualifiedName().typeName == requestedTypeName }
               when {
                  matchesOnNameOnly.isEmpty() -> Either.left(CompilationError(context.start, "Type $requestedTypeName could not be resolved"))
                  matchesOnNameOnly.size == 1 -> Either.right(matchesOnNameOnly.first().toQualifiedName())
                  else -> Either.left(CompilationError(context.start, "Type $requestedTypeName could not be resolved - it could refer to any of ${matchesOnNameOnly.joinToString { it.qualifiedName }}. Use an import to disambiguate."))
               }
            }
         }
      }
   }

   private fun fromTypeFromClassTypeContext(requestedTypeName: String, context: ParserRuleContext): Either<CompilationError, Type> {
      val imports = context.importsInFile().filter { it.typeName == requestedTypeName }
      val fullyQualifiedTypeName = if (imports.size == 1) imports.first().fullyQualifiedName else requestedTypeName
      return if (taxi.containsType(fullyQualifiedTypeName)) {
         Either.right(taxi.type(fullyQualifiedTypeName))
      } else {
         Either.left(CompilationError(context.start, "Type $requestedTypeName could not be resolved - so can't interpret anonymous projection definition."))
      }
   }

   private fun buildArrayType(context: ParserRuleContext): Either<CompilationError, QualifiedName> {
      return when (context) {
         is VyneQLParser.TypeTypeContext -> {
            val argumentContexts = context.typeArguments().typeType()
            val argumentType = when (argumentContexts.size) {
               0 -> PrimitiveType.ANY.toQualifiedName().right()
               1 -> lookupType(argumentContexts[0])
               else -> CompilationError(context.start, "Arrays can only have a single paramter type").left()
            }
            argumentType.map { argumentTypeName -> ArrayType.of(taxi.type(argumentTypeName)).toQualifiedName() }
         }
         else -> {
            TODO("Unhandled building array type from context type of ${context::class.simpleName}")
         }
      }
   }

   fun tokens(): Tokens {
      TODO()
   }
}


fun RuleContext.importsInFile(): List<QualifiedName> {
   val topLevel = this.searchUpForRule(listOf(VyneQLParser.QueryDocumentContext::class.java))
   if (topLevel == null || topLevel !is ParserRuleContext) {
      return emptyList()
   }
   val imports = topLevel.children.filterIsInstance<VyneQLParser.ImportDeclarationContext>()
      .map { QualifiedName.from(it.qualifiedName().Identifier().text()) }
   return imports
}

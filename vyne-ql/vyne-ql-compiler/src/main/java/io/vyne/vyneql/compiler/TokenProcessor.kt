package io.vyne.vyneql.compiler

import arrow.core.Either
import arrow.core.flatMap
import io.vyne.VyneQLBaseListener
import io.vyne.VyneQLParser
import io.vyne.vyneql.DiscoveryType
import io.vyne.vyneql.QueryMode
import io.vyne.vyneql.VyneQlQuery
import lang.taxi.*
import lang.taxi.types.Arrays
import lang.taxi.types.PrimitiveType
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type
import lang.taxi.utils.flattenErrors
import lang.taxi.utils.invertEitherList
import lang.taxi.utils.wrapErrorsInList
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RuleContext

class TokenProcessor(private val schema: TaxiDocument) : VyneQLBaseListener() {
   private val typeResolver: NamespaceQualifiedTypeResolver = object : NamespaceQualifiedTypeResolver {
      override val namespace: String = Namespaces.DEFAULT_NAMESPACE // We don't support ns in VyneQl yet

      override fun resolve(context: TaxiParser.TypeTypeContext) = error("Not supported")

      override fun resolve(requestedTypeName: String, context: ParserRuleContext): Either<CompilationError, Type> {
         return lookupTypeFromClassTypeContext(requestedTypeName, context)
            .map { schema.type(it) }
      }
   }
   private val constraintBuilder = ConstraintBuilder(schema, typeResolver)

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

      val query: Either<List<CompilationError>, VyneQlQuery> = parseQueryTypeList(ctx.queryTypeList()).flatMap { typesToDiscover ->
         parseTypeToProject(ctx.queryProjection())
            .wrapErrorsInList()
            .map { typeToProject ->
               VyneQlQuery(
                  name = name,
                  queryMode = queryDirective,
                  parameters = parameters,
                  typesToFind = typesToDiscover,
                  projectedType = typeToProject
               )
            }
      }
      return query
   }

   private fun parseTypeToProject(queryProjection: VyneQLParser.QueryProjectionContext?): Either<CompilationError, QualifiedName?> {
      if (queryProjection == null) {
         return Either.right(null)
      }
      return lookupType(queryProjection.typeType())
   }

   private fun parseQueryTypeList(queryTypeList: VyneQLParser.QueryTypeListContext): Either<List<CompilationError>, List<DiscoveryType>> {
      return queryTypeList.typeType().map { queryType ->
         lookupType(queryType)
            .mapLeft { listOf(it) }
            .flatMap { qualifiedName ->
               val type = schema.type(qualifiedName.parameterizedName)
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
         typeType.primitiveType() != null -> Either.right(PrimitiveType.fromDeclaration(typeType.primitiveType().text).toQualifiedName())
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
      return if (schema.containsType(requestedTypeName)) {
         Either.right(QualifiedName.from(requestedTypeName))
      } else {
         // Is there an explicit import?
         val imports = context.importsInFile().filter { it.typeName == requestedTypeName }
         when  {
            imports.size == 1 -> Either.right(imports.first())
            imports.size >1 -> Either.left(CompilationError(context.start, "Type $requestedTypeName is ambiguous and could refer to any of ${imports.joinToString(",")}}"))
            else -> {
               // There were no matching imports.
               // Check to see if this type is resolvable by name only
               val matchesOnNameOnly = schema.types.filter { it.toQualifiedName().typeName == requestedTypeName }
               when {
                  matchesOnNameOnly.isEmpty() -> Either.left(CompilationError(context.start, "Type $requestedTypeName could not be resolved"))
                  matchesOnNameOnly.size == 1 -> Either.right(matchesOnNameOnly.first().toQualifiedName())
                  else -> Either.left(CompilationError(context.start, "Type $requestedTypeName could not be resolved - it could refer to any of ${matchesOnNameOnly.joinToString { it.qualifiedName }}. Use an import to disambiguate."))
               }
            }
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

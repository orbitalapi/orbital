package com.orbitalhq

import com.google.common.annotations.VisibleForTesting
import com.orbitalhq.models.DefinedInSchema
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObjectFactory
import com.orbitalhq.models.facts.ScopedFact
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.models.json.addKeyValuePair
import com.orbitalhq.query.*
import com.orbitalhq.query.graph.Algorithms
import com.orbitalhq.schemas.*
import com.orbitalhq.schemas.taxi.TaxiConstraintConverter
import com.orbitalhq.schemas.taxi.TaxiSchemaAggregator
import com.orbitalhq.schemas.taxi.compileExpression
import com.orbitalhq.utils.Ids
import com.orbitalhq.utils.log
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import lang.taxi.accessors.ProjectionFunctionScope
import lang.taxi.query.FactValue
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.StreamType
import lang.taxi.types.UnionType
import java.util.*

enum class NodeTypes {
   ATTRIBUTE,
   TYPE,
   OBJECT,
   SERVICE
}

typealias OperationReference = String

interface SchemaContainer {
   val schema: Schema
   fun getType(typeName: String): Type = schema.type(typeName)
}


interface ModelContainer : SchemaContainer {
   fun addModel(model: TypedInstance, factSetId: FactSetId = FactSets.DEFAULT): ModelContainer
}

class Vyne(
   schemas: List<Schema>,
   private val queryEngineFactory: QueryEngineFactory,
   private val formatSpecs: List<ModelFormatSpec> = emptyList()
) : ModelContainer {

   init {
      if (schemas.size > 1) {
         error("Passing multiple schemas into Vyne is not supported anymore.  Pass a single composite schema")
      }
   }

   private val factSets: FactSetMap = FactSetMap.create()

   override var schema: Schema = schemas.firstOrNull() ?: SimpleSchema.EMPTY
      // Setter only for legacy purposes, used in tests we need to migrate.
      // schema is immutable now.
      private set

   fun queryEngine(
      factSetIds: Set<FactSetId> = setOf(FactSets.ALL),
      additionalFacts: Set<TypedInstance> = emptySet(),
      schema: Schema = this.schema
   ): StatefulQueryEngine {
      val factSetForQueryEngine: FactSetMap = FactSetMap.create()
      factSetForQueryEngine.putAll(this.factSets.retainFactsFromFactSet(factSetIds))
      factSetForQueryEngine.putAll(FactSets.DEFAULT, additionalFacts)
      return queryEngineFactory.queryEngine(schema, factSetForQueryEngine)
   }

   suspend fun query(
      vyneQlQuery: TaxiQLQueryString,
      queryId: String = UUID.randomUUID().toString(),
      clientQueryId: String? = null,
      eventBroker: QueryContextEventBroker = QueryContextEventBroker(),
      arguments: Map<String, Any?> = emptyMap()
   ): QueryResult {
      val (taxiQlQuery, queryOptions) = parseQuery(vyneQlQuery)
      return query(taxiQlQuery, queryId, clientQueryId, eventBroker, arguments, queryOptions = queryOptions)
   }


   fun parseQuery(vyneQlQuery: TaxiQLQueryString): Pair<TaxiQlQuery, QueryOptions> {
      return this.schema.parseQuery(vyneQlQuery)
   }

   suspend fun query(
      taxiQl: TaxiQlQuery,
      queryId: String = UUID.randomUUID().toString(),
      clientQueryId: String? = null,
      eventBroker: QueryContextEventBroker = QueryContextEventBroker(),
      arguments: Map<String, Any?> = emptyMap(),
      queryOptions: QueryOptions
   ): QueryResult {
      val currentJob = currentCoroutineContext().job
      val (queryContext, expression) = buildContextAndExpression(taxiQl, queryId, clientQueryId, eventBroker, arguments, queryOptions)
      val queryCanceller = QueryCanceller(queryContext, currentJob)
      eventBroker.addHandler(queryCanceller)
      return when (taxiQl.queryMode) {
         lang.taxi.query.QueryMode.FIND_ALL -> queryContext.findAll(expression)
         lang.taxi.query.QueryMode.FIND_ONE -> queryContext.find(expression)
         lang.taxi.query.QueryMode.STREAM -> queryContext.findAll(expression)
         lang.taxi.query.QueryMode.MAP -> queryContext.doMap(expression)
         lang.taxi.query.QueryMode.MUTATE -> queryContext.mutate(expression as MutatingQueryExpression)
      }
   }

   @VisibleForTesting
   internal fun deriveResponseType(taxiQl: TaxiQlQuery): String {

      return taxiQl.unwrappedReturnType.qualifiedName
      // 25-Jul-23: Was below.  I think the new impl. is more correct (and encapsulated),
      // but might've missed some edge cases.
      // Important to note the below did not consider the return type of mutations.
//      return taxiQl.projectedType?.let { projectedType ->
//         val type = ProjectionAnonymousTypeProvider.projectedTo(projectedType, schema)
//         type.collectionType?.fullyQualifiedName ?: type.fullyQualifiedName
//      } ?: taxiQl.typesToFind.first().typeName.firstTypeParameterOrSelf
   }

   @VisibleForTesting
   internal fun buildContextAndExpression(
      taxiQl: TaxiQlQuery,
      queryId: String,
      clientQueryId: String?,
      eventBroker: QueryContextEventBroker = QueryContextEventBroker(),
      arguments: Map<String, Any?> = emptyMap(),
      queryOptions: QueryOptions,
   ): Pair<QueryContext, QueryExpression> {

      // The facts in taxiQL are the variables defined in a given {} block.
      // given allows declaration in two ways:
      // given { foo : Foo = 123 }
      // and...
      // query( foo : Foo ) { // arguments passed at query runtime
      // given { foo }
      //
      // In the latter, we need to resolve foo against the arguments provided.

      val additionalFacts = taxiQl.facts.map { variable ->
         val argumentValue = variable.resolveValue(arguments)

         variable.name to TypedInstance.from(
            schema.type(argumentValue.fqn.parameterizedName),
            argumentValue.value,
            schema,
            source = Provided
         )
      }.toMap()

      // TODO : Do we need to remove the arguments here that were used in the given {} block?
      // I don't see why we would, but lets keep an eye...
      val scopedFacts = extractArgumentsFromQuery(taxiQl, arguments, formatSpecs)

      val inlineTypes = findInlineTypesInQuery(taxiQl, schema)

      val queryContext = query(
         additionalFacts = additionalFacts.values.toSet(),
         queryId = queryId,
         clientQueryId = clientQueryId,
         eventBroker = eventBroker,
         scopedFacts = scopedFacts,
         inlineTypes = inlineTypes,
         queryOptions = queryOptions
      )
         .responseType(deriveResponseType(taxiQl))
//      queryContext = taxiQl.projectedType?.let {
//         queryContext.projectResultsTo(it, taxiQl.projectionScope) // Merge conflict, was : it.toVyneQualifiedName()
//      } ?: queryContext

      val constraintProvider = TaxiConstraintConverter(this.schema)
      val queryExpressions = taxiQl.typesToFind.map { discoveryType ->

         val targetType = when {
            discoveryType.anonymousType != null -> ProjectionAnonymousTypeProvider.toVyneAnonymousType(discoveryType.anonymousType!!, schema)
            StreamType.isStreamTypeName(discoveryType.typeName) && UnionType.isUnionType(discoveryType.type.typeParameters()[0]) -> {
               val unionType = ProjectionAnonymousTypeProvider.toVyneStreamOfAnonymousType(discoveryType.type, schema)
               unionType
            }
            else -> schema.type(discoveryType.typeName.toVyneQualifiedName())
         }
         val expression = if (discoveryType.constraints.isNotEmpty()) {
            val constraints = constraintProvider.buildOutputConstraints(targetType, discoveryType.constraints)
            ConstrainedTypeNameQueryExpression(targetType.name.parameterizedName, constraints)
         } else {
            TypeQueryExpression(targetType)
         }

         expression
      }

      val expression: QueryExpression = when {
         queryExpressions.size > 1 -> {
            val streamJoin =  (queryExpressions.all { it is TypeQueryExpression && it.type.isStream })
            require(streamJoin) { "Multiple source types are only supported when joining streams" }
            StreamJoiningExpression(queryExpressions as List<TypeQueryExpression>)
               .applyProjection(taxiQl.projectedType, taxiQl.projectionScope, schema)
         }
         queryExpressions.size == 1 -> queryExpressions.first().let { expression ->
            expression.applyProjection(taxiQl.projectedType, taxiQl.projectionScope, schema)
         }

         else -> null
      }.let { possibleQueryExpression ->
         // At this point we have either:
         // Mutation -only query.
         // Query-only query.
         // Query-then-mutate query.
         // Decorate encapsulates those and returns the correct expression
         MutatingQueryExpression.decorate(possibleQueryExpression, taxiQl.mutation)
      }



      return Pair(queryContext, expression)
   }

   /**
    * Returns a list of types that are defined in the query,
    * ie., not present in the base schema we're using
    */
   private fun findInlineTypesInQuery(taxiQl: TaxiQlQuery, schema: Schema): List<Type> {
      val inlineDiscoveryTypes: List<lang.taxi.types.Type> = taxiQl.typesToFind.flatMap { discoveryType ->
         schema.findUnknownTypes(discoveryType.type)
      }
      val inlineProjectionType: List<lang.taxi.types.Type> = taxiQl.projectedType?.let { schema.findUnknownTypes(it) } ?: emptyList()

      val vyneTypes = (inlineDiscoveryTypes + inlineProjectionType).map {
         schema.typeCreateIfRequired(it)
      }
      return vyneTypes

   }

   private fun extractArgumentsFromQuery(
      taxiQl: TaxiQlQuery,
      arguments: Map<String, Any?>,
      formatSpecs: List<ModelFormatSpec>
   ) = taxiQl.parameters.map { parameter ->
      val argValue = when {
         arguments.containsKey(parameter.name) -> TypedInstance.from(
            schema.type(parameter.type),
            arguments[parameter.name],
            schema,
            source = Provided,
            formatSpecs = formatSpecs
         )

         parameter.value is FactValue.Constant -> TypedInstance.from(
            schema.type(parameter.type),
            parameter.value.typedValue.value,
            schema,
            source = DefinedInSchema,
            formatSpecs = formatSpecs
         )

         else -> error("No value was provided for parameter ${parameter.name} ")
      }
      ScopedFact(ProjectionFunctionScope(parameter.name, parameter.type), argValue)
   }

   suspend fun evaluate(taxiExpression: String, returnType: Type): TypedInstance {
      val (schemaWithType, expressionType) = this.schema.compileExpression(taxiExpression, returnType)

      val queryContext = queryEngine(schema = schemaWithType)
         .queryContext(queryId = Ids.id("queryId"), clientQueryId = null)

      // Using TypedObjectFactory directly, rather than queryEngine().build(...).
      // This is because of a bug that if the fact we're searching is a collection,
      // The projection logic is incorrectly attempting to project each of the items within the collection
      // to our predicate.
      // That's wrong, as generally the collection will be the input, especially if our predciate / expression
      // is a contains(...)
      val buildResult = TypedObjectFactory(
         expressionType,
         queryContext.facts,
         schemaWithType,
         source = Provided,
         inPlaceQueryEngine = queryContext,
         functionResultCache = queryContext.functionResultCache
      ).build()
      return buildResult
   }

   fun query(
      factSetIds: Set<FactSetId> = setOf(FactSets.ALL),
      additionalFacts: Set<TypedInstance> = emptySet(),
      queryId: String = UUID.randomUUID().toString(),
      clientQueryId: String? = null,
      eventBroker: QueryContextEventBroker = QueryContextEventBroker(),
      scopedFacts: List<ScopedFact> = emptyList(),
      inlineTypes: List<Type> = emptyList(),
      queryOptions: QueryOptions = QueryOptions.default()
   ): QueryContext {

      val schemaWithInlineTypes = createSchemaWithInlineTypes(this.schema, inlineTypes)

      // Design note:  I'm creating the queryEngine with ALL the fact sets, regardless of
      // what is asked for, but only providing the desired factSets to the queryContext.
      // This is because the context only evalutates the factSets that are provided,
      // so we limit the set of fact sets to provide.
      // However, we may want to expand the set of factSets later, (eg., to include a caller
      // factSet), so leave them present in the queryEngine.
      // Hopefully, this lets us have the best of both worlds.
      val queryEngine = queryEngine(setOf(FactSets.ALL), additionalFacts, schema = schemaWithInlineTypes)
      return queryEngine.queryContext(
         factSetIds = factSetIds,
         queryId = queryId,
         clientQueryId = clientQueryId,
         eventBroker = eventBroker,
         scopedFacts = scopedFacts,
         queryOptions = queryOptions
      )
   }

   private fun createSchemaWithInlineTypes(schema: Schema, inlineTypes: List<Type>): Schema {
      if (inlineTypes.isEmpty()) {
         return schema
      } else {
         return QuerySchema(inlineTypes, schema)
      }
   }

   fun accessibleFrom(fullyQualifiedTypeName: String): Set<Type> {
      return Algorithms.accessibleThroughSingleArgumentFunctionInvocation(schema, fullyQualifiedTypeName)
   }


   //   fun queryContext(): QueryContext = QueryContext(schema, facts, this)
   constructor(
      queryEngineFactory: QueryEngineFactory = QueryEngineFactory.default(),
      formatSpecs: List<ModelFormatSpec> = emptyList()
   ) : this(
      emptyList(),
      queryEngineFactory,
      formatSpecs
   )

   override fun addModel(model: TypedInstance, factSetId: FactSetId): Vyne {
      log().debug("Added model instance to factSet $factSetId: ${model.type.fullyQualifiedName}")
      this.factSets[factSetId].add(model)
//      invalidateGraph()
      return this
   }

   @Deprecated("Schemas should not be mutable.  This only remains for tests")
   fun addSchema(schema: Schema): Vyne {
      if (this.schema != SimpleSchema.EMPTY) {
         error("Cannot change the schema after it's set, even in tests.  Rewrite your test.")
      }
      this.schema = schema
      return this
   }

   fun from(
      facts: Set<TypedInstance>,
      queryId: String = UUID.randomUUID().toString(),
      clientQueryId: String? = null,
      eventBroker: QueryContextEventBroker = QueryContextEventBroker()
   ): QueryContext {
      return query(
         additionalFacts = facts,
         queryId = queryId,
         clientQueryId = clientQueryId,
         eventBroker = eventBroker
      )
   }

   fun from(
      fact: TypedInstance,
      queryId: String = UUID.randomUUID().toString(),
      clientQueryId: String? = null,
      eventBroker: QueryContextEventBroker = QueryContextEventBroker()
   ): QueryContext {
      return query(
         additionalFacts = setOf(fact),
         queryId = queryId,
         clientQueryId = clientQueryId,
         eventBroker = eventBroker
      )
   }

   //   fun getType(typeName: String): Type = schema.type(typeName)
   fun type(typeName: String): Type = getType(typeName)

   fun type(typeReference: VersionedTypeReference): Type {
      // TODO : Assert that the versions are the same, or
      // possibly even go and fetch the type if required
      log().warn("Not currently asserting type versions match")
      return getType(typeReference.typeName.fullyQualifiedName)
   }

   fun getService(serviceName: String): Service = schema.service(serviceName)


   fun getPolicy(type: Type): Policy? {
      return schema.policy(type)
   }

   suspend fun execute(query: Query): QueryResult {
      query.facts.forEach { fact -> this.addKeyValuePair(fact.typeName, fact.value, fact.factSetId) }
      return when (query.queryMode) {
         QueryMode.DISCOVER -> this.query(queryId = query.queryId).find(query.expression)
         QueryMode.GATHER -> this.query(queryId = query.queryId).findAll(query.expression)
         QueryMode.BUILD -> this.query(queryId = query.queryId).build(query.expression)
      }
   }
}


/**
 * This idea should possibly be deprecated.
 * The original idea was that we'd store schemas in their various formats.
 * However, its looking more and more like we just convert all inbound sources to Taxi,
 * which certainly makes life siimpler.
 *
 * The current approach of SchemaAggregators don't do a good job of handling imports.
 *
 * Instead, CompositeSchema(TaxiSchema.from(List<NamedSource>)) is a better approach, as it DOES
 * correctly order for imports and root out circular depenenices.
 *
 * Update: 23-Jan-2020:
 * We're definitely moving away from supporting diffferent types of schemas, to only
 * supporting Taxi.
 * Taxi needs to evolve to keep pace with language features in other scheams,
 * but is sufficiently ahead of many other representations that we want to simplify
 * the Vyne side.
 * As we move more towards type extensions for collboration and general purpose extensibility
 * this becomes more important, as supporting type extensions cross-schema type is too hard,
 * given import rules etc.
 */
@Deprecated("Going away.")
interface SchemaAggregator {
   companion object {
      val DEFAULT_AGGREGATORS = listOf<SchemaAggregator>(TaxiSchemaAggregator())
   }

   /**
    * Returns an aggregated schema, and the
    * remaining, unaffected schemas
    */
   fun aggregate(schemas: List<Schema>): Pair<Schema?, List<Schema>>
}

class CompositeSchemaBuilder(val aggregators: List<SchemaAggregator> = SchemaAggregator.DEFAULT_AGGREGATORS) {
   fun aggregate(schemas: List<Schema>): Schema {
      var unaggregated = schemas
      val aggregatedSchemas = aggregators.mapNotNull { aggregator ->
         val (aggregated, remaining) = aggregator.aggregate(unaggregated)
         unaggregated = remaining
         /*return*/ aggregated
      }
      return CompositeSchema(unaggregated + aggregatedSchemas)
   }
}

fun QueryExpression.applyProjection(
   projectedType: lang.taxi.types.Type?,
   projectionScope: ProjectionFunctionScope?,
   schema: Schema
):QueryExpression {
   if (projectedType == null) {
      return this
   }
   return ProjectedExpression(
      this,
      Projection(
         ProjectionAnonymousTypeProvider.projectedTo(projectedType, schema),
         projectionScope,
      )
   )
}

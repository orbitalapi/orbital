package com.orbitalhq

import com.google.common.annotations.VisibleForTesting
import com.orbitalhq.models.AccessorReader
import com.orbitalhq.models.DefinedInSchema
import com.orbitalhq.models.FactBagValueSupplier
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.TypedObjectFactory
import com.orbitalhq.models.facts.CopyOnWriteFactBag
import com.orbitalhq.models.facts.ScopedFact
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.models.json.addKeyValuePair
import com.orbitalhq.query.Fact
import com.orbitalhq.query.MetricTags
import com.orbitalhq.query.MutatingQueryExpression
import com.orbitalhq.query.ProjectedExpression
import com.orbitalhq.query.Projection
import com.orbitalhq.query.ProjectionAnonymousTypeProvider
import com.orbitalhq.query.Query
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.QueryContextEventBroker
import com.orbitalhq.query.QueryEngineFactory
import com.orbitalhq.query.QueryExpression
import com.orbitalhq.query.QueryMode
import com.orbitalhq.query.QueryResult
import com.orbitalhq.query.QuerySchema
import com.orbitalhq.query.StatefulQueryEngine
import com.orbitalhq.query.graph.Algorithms
import com.orbitalhq.query.planner.QueryPlanner
import com.orbitalhq.schemas.CompositeSchema
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.SimpleSchema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.taxi.TaxiSchemaAggregator
import com.orbitalhq.schemas.taxi.compileExpression
import com.orbitalhq.schemas.taxi.toVyneQualifiedName
import com.orbitalhq.utils.Ids
import com.orbitalhq.utils.log
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import lang.taxi.accessors.ProjectionFunctionScope
import lang.taxi.expressions.ProjectingExpression
import lang.taxi.policies.Policy
import lang.taxi.query.FactValue
import lang.taxi.query.Parameter
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.StreamType
import lang.taxi.types.TypedValue
import java.util.UUID

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
   fun addAuth(model: TypedInstance): ModelContainer = addModel(model, FactSets.AUTHENTICATION)
}

class Vyne(
   schemas: List<Schema>,
   private val queryEngineFactory: QueryEngineFactory,
   private val formatSpecs: List<ModelFormatSpec> = emptyList(),
   private val queryPlanner: QueryPlanner = QueryPlanner(),
) : ModelContainer {

   init {
      if (schemas.size > 1) {
         error("Passing multiple schemas into Vyne is not supported anymore.  Pass a single composite schema")
      }
   }

   fun clone(schema: Schema = this.schema): Vyne {
      return Vyne(
         listOf(schema),
         queryEngineFactory, formatSpecs,
         queryPlanner
      )
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
       arguments: Map<String, Any?> = emptyMap(),
       metricsTags: MetricTags = MetricTags.NONE,
       executionContextFacts: Set<Fact> = emptySet()
   ): QueryResult {
      val (taxiQlQuery, queryOptions, querySchema) = parseQuery(vyneQlQuery)
      return query(
         taxiQlQuery,
         queryId,
         clientQueryId,
         eventBroker,
         arguments,
         queryOptions = queryOptions,
         metricsTags,
         querySchema = querySchema,
         executionContextFacts = executionContextFacts
      )
   }


   fun parseQuery(vyneQlQuery: TaxiQLQueryString): Triple<TaxiQlQuery, QueryOptions, Schema> {
      return this.schema.parseQuery(vyneQlQuery)
   }

   suspend fun query(
       taxiQl: TaxiQlQuery,
       queryId: String = UUID.randomUUID().toString(),
       clientQueryId: String? = null,
       eventBroker: QueryContextEventBroker = QueryContextEventBroker(),
       arguments: Map<String, Any?> = emptyMap(),
       queryOptions: QueryOptions,
       metricsTags: MetricTags = MetricTags.NONE,
       querySchema: Schema = schema,
       executionContextFacts: Set<Fact> = emptySet()
   ): QueryResult {
      val currentJob = currentCoroutineContext().job
      val (queryContext: QueryContext, expression: QueryExpression) = buildContextAndExpression(
         taxiQl,
         queryId,
         clientQueryId,
         eventBroker,
         arguments,
         queryOptions,
         querySchema = querySchema,
         executionContextFacts = executionContextFacts
      )
      val queryCanceller = QueryCanceller(queryContext, currentJob)
      eventBroker.addHandler(queryCanceller)
      return when (taxiQl.queryMode) {
         lang.taxi.query.QueryMode.FIND_ALL -> queryContext.findAll(expression, metricsTags = metricsTags)
         lang.taxi.query.QueryMode.FIND_ONE -> queryContext.find(expression, metricsTags = metricsTags)
         lang.taxi.query.QueryMode.STREAM -> queryContext.findAll(expression, metricsTags = metricsTags)
         lang.taxi.query.QueryMode.MAP -> queryContext.doMap(expression, metricsTags = metricsTags)
         lang.taxi.query.QueryMode.MUTATE -> queryContext.mutate(
            expression as MutatingQueryExpression,
            metricsTags = metricsTags
         )
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

   data class ConstructedQueryContext(
      val queryContext: QueryContext,
      val expression: QueryExpression,
      val taxiQl: TaxiQlQuery,
      val querySchema: Schema
   )

   private fun authFactsToScopedFacts(factsToFilter: Set<Fact>): List<ScopedFact> {
      val factsAsTypedInstances = factsToFilter.filter { it.factSetId == FactSets.AUTHENTICATION }
         .map { it.toTypedInstance(schema) }
      return authFactsToScopedFacts(factsAsTypedInstances)
   }
   private fun authFactsToScopedFacts(facts: Collection<TypedInstance>): List<ScopedFact> {
      return facts
         .mapIndexed { index, typedInstance ->
            ScopedFact(
               ProjectionFunctionScope("AuthenticationFact$index", typedInstance.type.taxiType),
               typedInstance
            )
         }
   }

   @VisibleForTesting
   internal fun buildContextAndExpression(
      taxiQl: TaxiQlQuery,
      queryId: String,
      clientQueryId: String?,
      eventBroker: QueryContextEventBroker = QueryContextEventBroker(),
      arguments: Map<String, Any?> = emptyMap(),
      queryOptions: QueryOptions,
      querySchema: Schema,
      executionContextFacts: Set<Fact> = emptySet()
   ): ConstructedQueryContext {

      val additionalFacts = convertTaxiQlFactToInstances(taxiQl, arguments, executionContextFacts)

      val givenFacts = taxiQl.facts.map { it.name }.toSet()
      val filteredParams = taxiQl.parameters.filter { !givenFacts.contains(it.name) }
      val scopedFacts = extractArgumentsFromQuery(filteredParams, arguments, formatSpecs)

      // Anything that was declared with a name in the given {} block
      // is also eligible as a named variable within the query itself,
      // so convert these to scoped facts
      val namedScopedFacts =
         additionalFacts.map { (name, value) -> ScopedFact(ProjectionFunctionScope(name, value.type.taxiType), value) }

      val (expression, amendedTaxiQlQuery, amendedQuerySchema) = queryPlanner.buildQueryExpression(taxiQl, querySchema)

      // Place the auth credentials (if present) as scoped facts.
      // This ensures that when we create new query contexts, they are carried over
      val authFacts = authFactsToScopedFacts(executionContextFacts)

      val queryContext = query(
         additionalFacts = additionalFacts.values.toSet(),
         queryId = queryId,
         clientQueryId = clientQueryId,
         eventBroker = eventBroker,
         scopedFacts = scopedFacts + namedScopedFacts + authFacts,
         queryOptions = queryOptions,
         querySchema = amendedQuerySchema
      )
         .responseTypeName(deriveResponseType(taxiQl))


      return ConstructedQueryContext(
         queryContext, expression, amendedTaxiQlQuery, amendedQuerySchema
      )
   }

   private fun convertTaxiQlFactToInstances(
      taxiQl: TaxiQlQuery,
      arguments: Map<String, Any?>,
      executionContextFacts: Set<Fact> = emptySet()
   ): Map<String, TypedInstance> {
      // The facts in taxiQL are the variables defined in a given {} block.
      // given allows declaration in three ways:
      // 1: given { foo : Foo = 123 }
      // 2 : query( foo : Foo ) { // arguments passed at query runtime
      // given { foo }
      //
      // 3: given { a : Foo = someExpression() } // values defined from expressions

      // use-cases 1+2 are resolved simply here:
      val constants = taxiQl.facts
         .filter { it.value !is FactValue.Expression }
         .map { variable ->
            val argumentValue = executionContextFacts.filter { fact ->
               fact.qualifiedName == variable.type.toVyneQualifiedName()
            }.map { fact -> TypedValue(schema.taxiType(fact.qualifiedName), fact.value) }.firstOrNull()
               ?: variable.resolveValue(arguments)

            val typedInstance = TypedInstance.from(
               schema.type(argumentValue.fqn.parameterizedName),
               argumentValue.value,
               schema,
               source = Provided,
               formatSpecs = this.formatSpecs
            )
            ScopedFact(ProjectionFunctionScope(variable.name, typedInstance.type.taxiType), typedInstance)
//            variable.name to typedInstance
         }
      // usecase 3 (expressions) requires us to ??
      // A Bit of hoop jumping so that the values already provided
      // to us are available in the expressions we're evaluating.
      // eg:
      // given { name : String = 'foo' , upper = upperCase(name) }
      val evaluatedExpressions = taxiQl.facts
         .filter { it.value is FactValue.Expression }
         .fold(constants) { previousParams, parameter ->
            val facts = CopyOnWriteFactBag(emptyList(), schema, previousParams)
            val valueSupplier = FactBagValueSupplier(facts, schema)
            val accessorReader = AccessorReader(
               valueSupplier,
               schema.functionRegistry,
               schema
            )

            val expression = parameter.value as FactValue.Expression
            val evaluationResult = accessorReader.evaluate(
               value = facts,
               returnType = schema.type(parameter.type),
               expression = expression.expression,
               format = null,
               dataSource = Provided
            )
            previousParams + ScopedFact(ProjectionFunctionScope(parameter.name, parameter.type), evaluationResult)
         }
      return evaluatedExpressions.map { it.scope.name to it.fact }
         .toMap()

   }

   private fun userPrincipalFact(fact: FactValue): Boolean {
      return fact.type.inheritsFrom(this.type(AuthClaimType.AuthClaimsTypeName.fullyQualifiedName).taxiType)
   }

   /**
    * Returns a list of types that are defined in the query,
    * ie., not present in the base schema we're using
    */
   private fun findInlineTypesInQuery(taxiQl: TaxiQlQuery, schema: Schema): List<Type> {
      val inlineDiscoveryTypes: List<lang.taxi.types.Type> = taxiQl.typesToFind.flatMap { discoveryType ->
         schema.findUnknownTypes(discoveryType.expression.returnType)
      }
      val inlineProjectionType: List<lang.taxi.types.Type> =
         taxiQl.projectedType?.let { schema.findUnknownTypes(it) } ?: emptyList()

      val vyneTypes = (inlineDiscoveryTypes + inlineProjectionType).map {
         schema.typeCreateIfRequired(it)
      }
      return vyneTypes

   }

   private fun extractArgumentsFromQuery(
      parameters: List<Parameter>,
      arguments: Map<String, Any?>,
      formatSpecs: List<ModelFormatSpec>
   ) = parameters.map { parameter ->
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

         !parameter.value.hasValue && parameter.nullable -> TypedNull.create(schema.type(parameter.type))

         else -> {
            error("No value was provided for parameter ${parameter.name} ")
         }
      }
      ScopedFact(ProjectionFunctionScope(parameter.name, parameter.type), argValue)
   }

   /**
    * Used for performing standalone policy evaluation - ie.,
    * outside the scope of a query.
    *
    * Normally, you don't need this
    */
   fun buildStandalonePolicyEvaluationScope(): QueryContext {
      val queryContext = queryEngine()
         .queryContext(
            queryId = Ids.id("queryId"),
            clientQueryId = null,
            scopedFacts = authFactsToScopedFacts(factSets[FactSets.AUTHENTICATION])
         )
      return queryContext
   }

   fun evaluate(taxiExpression: String, returnType: Type): TypedInstance {
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
      queryOptions: QueryOptions = QueryOptions.default(),
      querySchema: Schema = this.schema
   ): QueryContext {


      // Design note:  I'm creating the queryEngine with ALL the fact sets, regardless of
      // what is asked for, but only providing the desired factSets to the queryContext.
      // This is because the context only evalutates the factSets that are provided,
      // so we limit the set of fact sets to provide.
      // However, we may want to expand the set of factSets later, (eg., to include a caller
      // factSet), so leave them present in the queryEngine.
      // Hopefully, this lets us have the best of both worlds.
      val queryEngine = queryEngine(setOf(FactSets.ALL), additionalFacts, schema = querySchema)
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

   override fun addAuth(model: TypedInstance): Vyne {
      return addModel(model, FactSets.AUTHENTICATION)
   }

   override fun addModel(model: TypedInstance, factSetId: FactSetId): Vyne {
      log().debug("Added model instance to factSet $factSetId: ${model.type.fullyQualifiedName}")
      this.factSets[factSetId].add(model)
//      invalidateGraph()
      return this
   }

   fun removeModel(model: TypedInstance, factSetId: FactSetId = FactSets.DEFAULT): Vyne {
      this.factSets[factSetId].remove(model)
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
      return schema.singlePolicyOrNull(type)
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
   expression: ProjectingExpression?,
   schema: Schema
): QueryExpression {
   if (expression == null) {
      return this
   }

   val unwrappedScope= expression.projection.projectionFunctionScope.map { scopeMember ->
      // When building a projection, streams that are in scope should be unpacked to their individual items.
      // Otherwise, when we're projecting a stream the scope contains a reference to the stream itself, not the emitted items.
      // Given we don't have an instance of the stream, this triggers another discovery, searching for the stream again.
      if (scopeMember.type is StreamType) {
         scopeMember.copy(type = (scopeMember.type as StreamType).type)
      } else {
         scopeMember
      }
   }
   return ProjectedExpression(
      this,
      Projection(
         // MP 16-May-25:
         // expression.projection.projectionType correctly handles
         // edge cases, like a stream is actually projected as an array, not a stream,
         // even though it returns a Stream
         ProjectionAnonymousTypeProvider.projectedTo(expression.projection.projectedType, schema),
         unwrappedScope,
         expression
      )
   )
}

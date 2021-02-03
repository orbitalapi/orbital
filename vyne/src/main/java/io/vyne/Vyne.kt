package io.vyne

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.google.common.annotations.VisibleForTesting
import io.vyne.models.TypedInstance
import io.vyne.models.json.addKeyValuePair
import io.vyne.query.ConstrainedTypeNameQueryExpression
import io.vyne.query.Query
import io.vyne.query.QueryContext
import io.vyne.query.QueryEngineFactory
import io.vyne.query.QueryExpression
import io.vyne.query.QueryMode
import io.vyne.query.QueryResult
import io.vyne.query.StatefulQueryEngine
import io.vyne.query.TypeNameQueryExpression
import io.vyne.query.graph.Algorithms
import io.vyne.schemas.CompositeSchema
import io.vyne.schemas.Policy
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.QualifiedNameAsStringSerializer
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import io.vyne.schemas.taxi.TaxiConstraintConverter
import io.vyne.schemas.taxi.TaxiSchemaAggregator
import io.vyne.schemas.toVyneQualifiedName
import io.vyne.utils.log
import io.vyne.vyneql.VyneQLQueryString
import io.vyne.vyneql.VyneQlCompiler
import io.vyne.vyneql.VyneQlQuery
import reactor.core.publisher.Flux
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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

data class RunningQueryStatus(
   val queryId: String,
   val vyneQlQuery: VyneQLQueryString,
   @JsonSerialize(using = QualifiedNameAsStringSerializer::class)
   val responseTypeName: QualifiedName,
   val completedProjections: Int,
   val estimatedProjectionCount: Int?,
   val startTime: Instant,
   val running: Boolean
)

data class ExecutableQuery(
   @get:JsonIgnore
   val queryContext: QueryContext,
   val query: VyneQLQueryString,
   @get:JsonIgnore
   val parsedQuery: VyneQlQuery,
   @get:JsonIgnore
   val result: CompletableFuture<QueryResult>
) {
   fun resultStream(): Flux<TypedInstance> {
      return queryContext.resultStream
   }

   /**
    * Returns a flux that emits an updated query status each time
    * the result stream emits a new message
    */
   fun currentStatusStream(): Flux<RunningQueryStatus> {
      return Flux.from(queryContext.resultStream)
         .map { currentStatus() }
   }

   fun currentStatus(): RunningQueryStatus {
      val responseTypeName = when {
         parsedQuery.projectedType == null -> parsedQuery.typesToFind[0].type
         parsedQuery.projectedType?.concreteTypeName != null -> parsedQuery.projectedType!!.concreteTypeName!!
         parsedQuery.projectedType?.anonymousTypeDefinition != null -> {
            TODO("Serhat, what do I put here?  How do I find the name of the anonymous type?")
         }
         else -> error("Could not find response type name from VyneQl query")
      }
      return RunningQueryStatus(
         this.queryId,
         this.query,
         responseTypeName.toVyneQualifiedName(),
         this.completedProjections,
         this.estimatedProjectionCount,
         this.startTime,
         running = (!result.isDone && !result.isCancelled && !result.isCompletedExceptionally)
      )
   }

   fun stop(): Boolean {
      // This doesn't actually have any effect on the currently executing query - it just
      // marks the result as cancelled.
      // See https://stackoverflow.com/a/23329340/59015
//      this.result.cancel(true)
      this.queryContext.requestCancel()
      return this.queryContext.isCancelRequested

   }

   val queryId: String = queryContext.queryContextId
   val startTime = queryContext.executionStartTime

   val estimatedProjectionCount: Int?
      get() {
         return queryContext.projectionSize
      }
   val completedProjections: Int
      get() {
         return queryContext.completedProjections
      }
}

class Vyne(
   schemas: List<Schema>,
   private val queryEngineFactory: QueryEngineFactory,
   private val compositeSchemaBuilder: CompositeSchemaBuilder = CompositeSchemaBuilder(),
   private val executorService: ExecutorService = DEFAULT_EXECUTOR
) : ModelContainer {
   companion object {
      val DEFAULT_EXECUTOR = Executors.newFixedThreadPool(10)
   }

   private val schemas = mutableListOf<Schema>()

   private val factSets: FactSetMap = FactSetMap.create()

   override var schema: Schema = compositeSchemaBuilder.aggregate(schemas)
      private set

   fun queryEngine(
      factSetIds: Set<FactSetId> = setOf(FactSets.ALL),
      additionalFacts: Set<TypedInstance> = emptySet()
   ): StatefulQueryEngine {
      val factSetForQueryEngine: FactSetMap = FactSetMap.create()
      factSetForQueryEngine.putAll(this.factSets.filterFactSets(factSetIds))
      factSetForQueryEngine.putAll(FactSets.DEFAULT, additionalFacts)
      return queryEngineFactory.queryEngine(schema, factSetForQueryEngine)
   }

   fun query(vyneQlQuery: VyneQLQueryString): QueryResult {
      val vyneQuery = VyneQlCompiler(vyneQlQuery, this.schema.taxi).query()
      return query(vyneQuery)
   }

   fun query(vyneQl: VyneQlQuery): QueryResult {
      val (queryContext, expression) = buildContextAndExpression(vyneQl)
      return when (vyneQl.queryMode) {
         io.vyne.vyneql.QueryMode.FIND_ALL -> queryContext.findAll(expression)
         io.vyne.vyneql.QueryMode.FIND_ONE -> queryContext.find(expression)
      }
   }

   fun queryAsync(vyneQlQuery: VyneQLQueryString): ExecutableQuery {
      val vyneQuery: VyneQlQuery = VyneQlCompiler(vyneQlQuery, this.schema.taxi).query()
      val (queryContext, expression) = buildContextAndExpression(vyneQuery)

      val supplier: () -> QueryResult = when (vyneQuery.queryMode) {
         io.vyne.vyneql.QueryMode.FIND_ALL -> {
            { queryContext.findAll(expression) }
         }
         io.vyne.vyneql.QueryMode.FIND_ONE -> {
            { queryContext.find(expression) }
         }
      }
      val future = CompletableFuture.supplyAsync(supplier)
      return ExecutableQuery(queryContext, vyneQlQuery, vyneQuery, future)
   }

   @VisibleForTesting
   internal fun buildContextAndExpression(vyneQl: VyneQlQuery): Pair<QueryContext, QueryExpression> {
      var queryContext = query(additionalFacts = vyneQl.facts.values.toSet())
      queryContext = vyneQl.projectedType?.let {
         queryContext.projectResultsTo(it) // Merge conflict, was : it.toVyneQualifiedName()
      } ?: queryContext

      val constraintProvider = TaxiConstraintConverter(this.schema)
      val queryExpressions = vyneQl.typesToFind.map { discoveryType ->
         val targetType = schema.type(discoveryType.type.toVyneQualifiedName())
         val expression = if (discoveryType.constraints.isNotEmpty()) {
            val constraints = constraintProvider.buildOutputConstraints(targetType, discoveryType.constraints)
            ConstrainedTypeNameQueryExpression(targetType.name.parameterizedName, constraints)
         } else {
            TypeNameQueryExpression(discoveryType.type.toVyneQualifiedName().parameterizedName)
         }

         expression
      }

      if (queryExpressions.size > 1) {
         TODO("Handle multiple target types in VyneQL")
      }
      val expression = queryExpressions.first()
      return Pair(queryContext, expression)
   }

   fun query(
      factSetIds: Set<FactSetId> = setOf(FactSets.ALL),
      additionalFacts: Set<TypedInstance> = emptySet()
   ): QueryContext {
      // Design note:  I'm creating the queryEngine with ALL the fact sets, regardless of
      // what is asked for, but only providing the desired factSets to the queryContext.
      // This is because the context only evalutates the factSets that are provided,
      // so we limit the set of fact sets to provide.
      // However, we may want to expand the set of factSets later, (eg., to include a caller
      // factSet), so leave them present in the queryEngine.
      // Hopefully, this lets us have the best of both worlds.

      val queryEngine = queryEngine(setOf(FactSets.ALL), additionalFacts)
      return queryEngine.queryContext(factSetIds = factSetIds)
   }

   fun accessibleFrom(fullyQualifiedTypeName: String): Set<Type> {
      return Algorithms.accessibleFromThroughFunctionInvocations(schema, fullyQualifiedTypeName)
   }


   //   fun queryContext(): QueryContext = QueryContext(schema, facts, this)
   constructor(queryEngineFactory: QueryEngineFactory = QueryEngineFactory.default()) : this(
      emptyList(),
      queryEngineFactory
   )

   override fun addModel(model: TypedInstance, factSetId: FactSetId): Vyne {
      log().debug("Added model instance to factSet $factSetId: ${model.type.fullyQualifiedName}")
      this.factSets[factSetId].add(model)
//      invalidateGraph()
      return this
   }

   fun addSchema(schema: Schema): Vyne {
      schemas.add(schema)
      this.schema = CompositeSchema(schemas)
//      invalidateGraph()
      return this
   }

//   private fun invalidateGraph() {
//      this._graph = null
//   }

//   private fun rebuildGraph(): HipsterDirectedGraph<Element, Relationship> {
//      return VyneGraphBuilder(schema).build(models)
//   }


   fun from(fact: TypedInstance): QueryContext {
      return query(additionalFacts = setOf(fact))
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

   fun execute(query: Query): QueryResult {
      query.facts.forEach { fact -> this.addKeyValuePair(fact.typeName, fact.value, fact.factSetId) }
      return when (query.queryMode) {
         QueryMode.DISCOVER -> this.query().find(query.expression)
         QueryMode.GATHER -> this.query().findAll(query.expression)
         QueryMode.BUILD -> this.query().build(query.expression)
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

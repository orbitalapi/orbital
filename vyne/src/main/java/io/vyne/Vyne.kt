package io.vyne

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Stopwatch
import io.vyne.models.DefinedInSchema
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObjectFactory
import io.vyne.models.facts.ScopedFact
import io.vyne.models.json.addKeyValuePair
import io.vyne.query.*
import io.vyne.query.graph.Algorithms
import io.vyne.schemas.*
import io.vyne.schemas.taxi.TaxiConstraintConverter
import io.vyne.schemas.taxi.TaxiSchemaAggregator
import io.vyne.schemas.taxi.compileExpression
import io.vyne.utils.Ids
import io.vyne.utils.log
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import lang.taxi.Compiler
import lang.taxi.accessors.ProjectionFunctionScope
import lang.taxi.query.FactValue
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
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
   private val queryEngineFactory: QueryEngineFactory
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
      factSetForQueryEngine.putAll(this.factSets.filterFactSets(factSetIds))
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
      val taxiQlQuery = parseQuery(vyneQlQuery).first
      return query(taxiQlQuery, queryId, clientQueryId, eventBroker, arguments)
   }


   fun parseQuery(vyneQlQuery: TaxiQLQueryString): Pair<TaxiQlQuery, QueryOptions> {
      val sw = Stopwatch.createStarted()
      val vyneQuery = Compiler(source = vyneQlQuery, importSources = listOf(this.schema.taxi)).queries().first()
      log().debug("Compiled query in ${sw.elapsed().toMillis()}ms")
      return vyneQuery to QueryOptions.fromQuery(vyneQuery)
   }

   suspend fun query(
      taxiQl: TaxiQlQuery,
      queryId: String = UUID.randomUUID().toString(),
      clientQueryId: String? = null,
      eventBroker: QueryContextEventBroker = QueryContextEventBroker(),
      arguments: Map<String, Any?> = emptyMap()
   ): QueryResult {
      val currentJob = currentCoroutineContext().job
      val (queryContext, expression) = buildContextAndExpression(taxiQl, queryId, clientQueryId, eventBroker, arguments)
      val queryCanceller = QueryCanceller(queryContext, currentJob)
      eventBroker.addHandler(queryCanceller)
      return when (taxiQl.queryMode) {
         lang.taxi.query.QueryMode.FIND_ALL -> queryContext.findAll(expression)
         lang.taxi.query.QueryMode.FIND_ONE -> queryContext.find(expression)
         lang.taxi.query.QueryMode.STREAM -> queryContext.findAll(expression)
      }
   }

   @VisibleForTesting
   internal fun deriveResponseType(taxiQl: TaxiQlQuery): String {
      return taxiQl.projectedType?.let { projectedType ->
         val type = ProjectionAnonymousTypeProvider.projectedTo(projectedType, schema)
         type.collectionType?.fullyQualifiedName ?: type.fullyQualifiedName
      } ?: taxiQl.typesToFind.first().typeName.firstTypeParameterOrSelf
   }

   @VisibleForTesting
   internal fun buildContextAndExpression(
      taxiQl: TaxiQlQuery,
      queryId: String,
      clientQueryId: String?,
      eventBroker: QueryContextEventBroker = QueryContextEventBroker(),
      arguments: Map<String, Any?> = emptyMap(),
   ): Pair<QueryContext, QueryExpression> {
      val additionalFacts = taxiQl.facts.map { variable ->
         TypedInstance.from(
            schema.type(variable.value.typedValue.fqn.fullyQualifiedName),
            variable.value.typedValue.value,
            schema,
            source = Provided
         )
      }.toSet()
      val scopedFacts = extractArgumentsFromQuery(taxiQl, arguments)

      val queryContext = query(
         additionalFacts = additionalFacts,
         queryId = queryId,
         clientQueryId = clientQueryId,
         eventBroker = eventBroker,
         scopedFacts = scopedFacts
      )
         .responseType(deriveResponseType(taxiQl))
//      queryContext = taxiQl.projectedType?.let {
//         queryContext.projectResultsTo(it, taxiQl.projectionScope) // Merge conflict, was : it.toVyneQualifiedName()
//      } ?: queryContext

      val constraintProvider = TaxiConstraintConverter(this.schema)
      val queryExpressions = taxiQl.typesToFind.map { discoveryType ->
         val targetType = discoveryType.anonymousType?.let {
            ProjectionAnonymousTypeProvider
               .toVyneAnonymousType(discoveryType.anonymousType!!, schema)
         }
            ?: schema.type(discoveryType.typeName.toVyneQualifiedName())

         val expression = if (discoveryType.constraints.isNotEmpty()) {
            val constraints = constraintProvider.buildOutputConstraints(targetType, discoveryType.constraints)
            ConstrainedTypeNameQueryExpression(targetType.name.parameterizedName, constraints)
         } else {
            TypeNameQueryExpression(discoveryType.typeName.toVyneQualifiedName().parameterizedName)
         }

         expression
      }

      if (queryExpressions.size > 1) {
         TODO("Handle multiple target types in VyneQL")
      }
      val expression = queryExpressions.first().let { expression ->
         if (taxiQl.projectedType != null) {
            ProjectedExpression(
               expression,
               Projection(
                  ProjectionAnonymousTypeProvider.projectedTo(taxiQl.projectedType!!, schema),
                  taxiQl.projectionScope
               )
            )
         } else {
            expression
         }
      }

      return Pair(queryContext, expression)
   }

   private fun extractArgumentsFromQuery(
      taxiQl: TaxiQlQuery,
      arguments: Map<String, Any?>
   ) = taxiQl.parameters.map { parameter ->
      val argValue = when {
         arguments.containsKey(parameter.name) -> TypedInstance.from(
            schema.type(parameter.type),
            arguments[parameter.name],
            schema,
            source = Provided
         )

         parameter.value is FactValue.Constant -> TypedInstance.from(
            schema.type(parameter.type),
            parameter.value.typedValue.value,
            schema,
            source = DefinedInSchema
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
      scopedFacts: List<ScopedFact> = emptyList()
   ): QueryContext {

      // Design note:  I'm creating the queryEngine with ALL the fact sets, regardless of
      // what is asked for, but only providing the desired factSets to the queryContext.
      // This is because the context only evalutates the factSets that are provided,
      // so we limit the set of fact sets to provide.
      // However, we may want to expand the set of factSets later, (eg., to include a caller
      // factSet), so leave them present in the queryEngine.
      // Hopefully, this lets us have the best of both worlds.
      val queryEngine = queryEngine(setOf(FactSets.ALL), additionalFacts)
      return queryEngine.queryContext(
         factSetIds = factSetIds,
         queryId = queryId,
         clientQueryId = clientQueryId,
         eventBroker = eventBroker,
         scopedFacts = scopedFacts
      )
   }

   fun accessibleFrom(fullyQualifiedTypeName: String): Set<Type> {
      return Algorithms.accessibleThroughSingleArgumentFunctionInvocation(schema, fullyQualifiedTypeName)
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

package io.vyne

import com.google.common.annotations.VisibleForTesting
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.json.addKeyValuePair
import io.vyne.query.*
import io.vyne.query.graph.Algorithms
import io.vyne.schemas.*
import io.vyne.schemas.taxi.TaxiConstraintConverter
import io.vyne.schemas.taxi.TaxiSchemaAggregator
import io.vyne.utils.log
import lang.taxi.Compiler
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.TaxiQLQueryString
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

class Vyne(schemas: List<Schema>, private val queryEngineFactory: QueryEngineFactory, private val compositeSchemaBuilder: CompositeSchemaBuilder = CompositeSchemaBuilder()) : ModelContainer {

   init {
       if (schemas.size > 1) {
          error("Passing multiple schemas into Vyne is not supported anymore.  Pass a single composite schema")
       }
   }
   private val factSets: FactSetMap = FactSetMap.create()

   override var schema: Schema = schemas.firstOrNull() ?: SimpleSchema.EMPTY
      // Setter only for legacy purposes, used in tests we need to migrate.
      // schema is immuable now.
      private set;

   fun queryEngine(
      factSetIds: Set<FactSetId> = setOf(FactSets.ALL),
      additionalFacts: Set<TypedInstance> = emptySet()): StatefulQueryEngine {
      val factSetForQueryEngine: FactSetMap = FactSetMap.create()
      factSetForQueryEngine.putAll(this.factSets.filterFactSets(factSetIds))
      factSetForQueryEngine.putAll(FactSets.DEFAULT, additionalFacts)
      return queryEngineFactory.queryEngine(schema, factSetForQueryEngine)
   }

   suspend fun query(vyneQlQuery: TaxiQLQueryString, queryId: String = UUID.randomUUID().toString(), clientQueryId: String? = null, eventBroker: QueryContextEventBroker = QueryContextEventBroker()): QueryResult {
      val vyneQuery = Compiler(source = vyneQlQuery, importSources = listOf(this.schema.taxi)).queries().first()
      return query(vyneQuery, queryId, clientQueryId, eventBroker)
   }

   suspend fun query(taxiQl: TaxiQlQuery, queryId: String = UUID.randomUUID().toString(), clientQueryId: String? = null, eventBroker: QueryContextEventBroker = QueryContextEventBroker()): QueryResult {
      val (queryContext, expression) = buildContextAndExpression(taxiQl, queryId, clientQueryId, eventBroker)

      return when (taxiQl.queryMode) {
         lang.taxi.types.QueryMode.FIND_ALL -> queryContext.findAll(expression)
         lang.taxi.types.QueryMode.FIND_ONE -> queryContext.find(expression)
      }
   }

   @VisibleForTesting
   internal fun buildContextAndExpression(taxiQl: TaxiQlQuery, queryId: String, clientQueryId: String?, eventBroker: QueryContextEventBroker = QueryContextEventBroker()): Pair<QueryContext, QueryExpression> {
      val additionalFacts = taxiQl.facts.values.map { fact ->
         TypedInstance.from(schema.type(fact.fqn.fullyQualifiedName), fact.value, schema, source = Provided)
      }.toSet()
      var queryContext = query(additionalFacts = additionalFacts, queryId = queryId, clientQueryId = clientQueryId, eventBroker = eventBroker)
      queryContext = taxiQl.projectedType?.let {
         queryContext.projectResultsTo(it) // Merge conflict, was : it.toVyneQualifiedName()
      } ?: queryContext

      val constraintProvider = TaxiConstraintConverter(this.schema)
      val queryExpressions = taxiQl.typesToFind.map { discoveryType ->
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
      additionalFacts: Set<TypedInstance> = emptySet(),
      queryId: String = UUID.randomUUID().toString(),
      clientQueryId: String? = null,
      eventBroker: QueryContextEventBroker = QueryContextEventBroker()
   ): QueryContext {
      // Design note:  I'm creating the queryEngine with ALL the fact sets, regardless of
      // what is asked for, but only providing the desired factSets to the queryContext.
      // This is because the context only evalutates the factSets that are provided,
      // so we limit the set of fact sets to provide.
      // However, we may want to expand the set of factSets later, (eg., to include a caller
      // factSet), so leave them present in the queryEngine.
      // Hopefully, this lets us have the best of both worlds.
      val queryEngine = queryEngine(setOf(FactSets.ALL), additionalFacts)
      return queryEngine.queryContext(factSetIds = factSetIds, queryId = queryId, clientQueryId = clientQueryId, eventBroker = eventBroker)
   }

   fun accessibleFrom(fullyQualifiedTypeName: String): Set<Type> {
      return Algorithms.accessibleFromThroughFunctionInvocations(schema, fullyQualifiedTypeName)
   }


   //   fun queryContext(): QueryContext = QueryContext(schema, facts, this)
   constructor(queryEngineFactory: QueryEngineFactory = QueryEngineFactory.default()) : this(emptyList(), queryEngineFactory)

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

   fun from(fact: TypedInstance): QueryContext {
      return query(additionalFacts = setOf(fact), queryId = UUID.randomUUID().toString())
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

package io.vyne

import io.vyne.models.TypedInstance
import io.vyne.query.QueryContext
import io.vyne.query.QueryEngineFactory
import io.vyne.query.StatefulQueryEngine
import io.vyne.schemas.*
import io.vyne.schemas.taxi.TaxiSchemaAggregator
import io.vyne.utils.log

object GraphAttributes {
   val NODE_TYPE = "nodeType"
   val QUALIFIED_NAME = "qualifiedName"
   val EDGE_KEY = "edgeKey"
}

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
   private val schemas = mutableListOf<Schema>()
   //   private val models = mutableSetOf<TypedInstance>()
   private val factSets: FactSetMap = FactSetMap.create()


//   private var _graph: HipsterDirectedGraph<Element, Relationship>? = null
//   val graph: HipsterDirectedGraph<Element, Relationship>
//      get() {
//         if (_graph == null) {
//            this._graph = rebuildGraph()
//         }
//         return this._graph!!
//      }


   override var schema: Schema = compositeSchemaBuilder.aggregate(schemas)
      private set

   fun queryEngine(factSetIds: Set<FactSetId> = setOf(FactSets.ALL), additionalFacts: Set<TypedInstance> = emptySet()): StatefulQueryEngine {
      val factSetForQueryEngine: FactSetMap = FactSetMap.create()
      factSetForQueryEngine.putAll(this.factSets.filterFactSets(factSetIds))
      factSetForQueryEngine.putAll(FactSets.DEFAULT, additionalFacts)
      return queryEngineFactory.queryEngine(schema, factSetForQueryEngine)
   }

   fun query(factSetIds: Set<FactSetId> = setOf(FactSets.ALL), additionalFacts: Set<TypedInstance> = emptySet()): QueryContext {
      // Design note:  I'm creating the queryEngine with ALL the fact sets, regardless of
      // what is asked for, but only providing the desired factSets to the queryContext.
      // This is because the context only evalutates the factSets that are provided,
      // so we limit the set of fact sets to provide.
      // However, we may want to expand the set of factSets later, (eg., to include a caller
      // factSet), so leave them present in the queryEngine.
      // Hopefully, this lets us have the best of both worlds.

      val queryEngine = queryEngine(setOf(FactSets.ALL), additionalFacts)
      return queryEngine.queryContext(factSetIds)
   }


   //   fun queryContext(): QueryContext = QueryContext(schema, facts, this)
   constructor(queryEngineFactory: QueryEngineFactory = QueryEngineFactory.default()) : this(emptyList(), queryEngineFactory)

   override fun addModel(model: TypedInstance, factSetId: FactSetId): Vyne {
      log().debug("Added model instance to factSet $factSetId: $model")
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


   fun from(s: TypedInstance): QueryContext {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   //   fun getType(typeName: String): Type = schema.type(typeName)
   fun type(typeName: String): Type = getType(typeName)

   fun getService(serviceName: String): Service = schema.service(serviceName)


   fun getPolicy(type: Type): Policy? {
      return schema.policy(type)
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
 */
@Deprecated("Possibly going away.")
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

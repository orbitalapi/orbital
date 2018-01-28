package io.osmosis.polymer

import es.usc.citius.hipster.graph.GraphBuilder
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.query.QueryContext
import io.osmosis.polymer.query.QueryEngineFactory
import io.osmosis.polymer.query.StatefulQueryEngine
import io.osmosis.polymer.schemas.*
import io.osmosis.polymer.schemas.taxi.TaxiSchemaAggregator
import io.osmosis.polymer.utils.log

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
   fun addModel(model: TypedInstance): ModelContainer
}

class Polymer(schemas: List<Schema>, private val queryEngineFactory: QueryEngineFactory, private val compositeSchemaBuilder: CompositeSchemaBuilder = CompositeSchemaBuilder()) : ModelContainer {
   private val schemas = mutableListOf<Schema>()
   private val models = mutableSetOf<TypedInstance>()
   private var graph = GraphBuilder.create<Element, Relationship>().createDirectedGraph()

   override var schema: Schema = compositeSchemaBuilder.aggregate(schemas)
      private set

   fun query(): StatefulQueryEngine {
      return queryEngineFactory.queryEngine(schema, models)
   }

   fun models(): Set<TypedInstance> {
      return models.toSet()
   }

   //   fun queryContext(): QueryContext = QueryContext(schema, facts, this)
   constructor(queryEngineFactory: QueryEngineFactory = QueryEngineFactory.default()) : this(emptyList(), queryEngineFactory)

   override fun addModel(model: TypedInstance): Polymer {
      log().debug("Added model instance to context: $model")
      models.add(model)
      return this
   }

   fun addSchema(schema: Schema): Polymer {
      schemas.add(schema)
      this.schema = CompositeSchema(schemas)
      resetGraph(schema)
      return this
   }

   private fun resetGraph(schema: Schema) {
      this.graph = PolymerGraphBuilder(schema).build()
   }


   fun from(s: TypedInstance): QueryContext {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   //   fun getType(typeName: String): Type = schema.type(typeName)
   fun type(typeName: String): Type = getType(typeName)

   fun getService(serviceName: String): Service = schema.service(serviceName)
}


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
      val aggregatedSchemas = aggregators.map { aggregator ->
         val (aggregated, remaining) = aggregator.aggregate(unaggregated)
         unaggregated = remaining
         /*return*/ aggregated
      }.filterNotNull()
      return CompositeSchema(unaggregated + aggregatedSchemas)
   }
}

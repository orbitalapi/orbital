package io.vyne

import es.usc.citius.hipster.graph.GraphBuilder
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
   fun addModel(model: TypedInstance): ModelContainer
}

class Vyne(schemas: List<Schema>, private val queryEngineFactory: QueryEngineFactory, private val compositeSchemaBuilder: CompositeSchemaBuilder = CompositeSchemaBuilder()) : ModelContainer {
   private val schemas = mutableListOf<Schema>()
   private val models = mutableSetOf<TypedInstance>()
   var graph = GraphBuilder.create<Element, Relationship>().createDirectedGraph()
      private set;

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

   override fun addModel(model: TypedInstance): Vyne {
      log().debug("Added model instance to context: $model")
      models.add(model)
      resetGraph()
      return this
   }

   fun addSchema(schema: Schema): Vyne {
      schemas.add(schema)
      this.schema = CompositeSchema(schemas)
      resetGraph()
      return this
   }

   private fun resetGraph() {
      this.graph = VyneGraphBuilder(schema).build(models)
   }


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

package io.vyne.query

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.models.DataSource
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.full.memberProperties


/**
 * This is a marker class, which we use as a serialization hack.
 * At serialization time, we can conditionally register a different serializer,
 * which will swap out this object with a graph from the query result
 */
object LineageGraph

object Lineage {
   fun newLineageAwareJsonMapper(dataSourceRegistry: DataSourceRegistry = DataSourceRegistry()):ObjectMapper = jacksonObjectMapper()
      .registerModule(VyneJacksonModule())
      .registerModule(TaxiJacksonModule())
      .registerModule(JavaTimeModule())
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .registerModule(LineageGraphSerializationModule(dataSourceRegistry))
}
class DataSourceRegistry {
   val counter = AtomicLong(0)
   private val nestedMapper: ObjectMapper = Lineage.newLineageAwareJsonMapper(this)
   private val sources = mutableMapOf<DataSource, Long>()
   internal val sourceTrees = mutableMapOf<Long, Any>()

   fun register(datasource: DataSource): Long {
      // This is not threadsafe, but we shouldn't be sharing instances.
      // Using computeIfAbsent leads to concurrentModificationExceptions
      // when recursing through nested objects
      if (!sources.containsKey(datasource)) {
         val index = counter.getAndIncrement()
         // Inspect the data source, to see what it's attributes are.
         // We'll store them into a map, so that later when we want
         // to serialzie out the reference to the data source we can.
         // Note - using reflection here isn't ideal because there's a risk
         // properties which Jackson wouldn't serialize (eg., @JsonIgnored properties)
         // get handled incorrectly.
         // However, I couldn't find a way to get the list of properties and values
         // out of jackson.
         val sourceTree = datasource::class.memberProperties.map { property ->
            val value = property.getter.call(datasource)?.let { propertyValue ->

               // This is the magic.
               // By calling back into the nested mapper (which is a different instance from the
               // root jackson mapper), but which shares the data source registry,
               // this ensures that any nested data sources get captured
               // and their values stored.
               nestedMapper.convertValue<Any>(propertyValue)
            }
            val name = property.name
            name to value
         }.toMap()
         sources[datasource] = index
         sourceTrees[index] = sourceTree
      }

      return sources.getValue(datasource)
   }


}

/**
 * This serialization module is stateful, and NOT threadsafe.
 * You must instantiate an instance per serialization usage
 */
class LineageGraphSerializationModule(registry: DataSourceRegistry = DataSourceRegistry()) : SimpleModule() {

   init {
      addSerializer(DataSource::class.java, DataSourceAsReferenceSerializer(registry))
      addSerializer(LineageGraph::class.java, LineageSerializer(registry))
   }
}

/**
 * This class swaps out instances of DataSource with a unique index.
 * If the same data source appears multiple times, it will be given the same
 * index.
 * Instances are collected onto the data source registry, which can
 * then be serialized out later, to provide the expanded references
 */
class DataSourceAsReferenceSerializer(val registry: DataSourceRegistry) : StdSerializer<DataSource>(DataSource::class.java) {

   override fun serialize(value: DataSource, gen: JsonGenerator, serializers: SerializerProvider) {
      val dataSourceIndex = registry.register(value)
      gen.writeObject(LineageReference(dataSourceIndex))
   }
}

data class LineageReference(val dataSourceIndex: Long)

/**
 * Responsible for actually writing out the lineage graph,
 * now that it's been computed.
 */
class LineageSerializer(val registry: DataSourceRegistry) : JsonSerializer<LineageGraph>() {
   override fun serialize(value: LineageGraph, gen: JsonGenerator, serializers: SerializerProvider) {
      gen.writeStartObject()
      registry.sourceTrees.forEach { (id, dataSource) ->
         serializers.defaultSerializeField(id.toString(), dataSource, gen)
      }
      gen.writeEndObject()
   }

}

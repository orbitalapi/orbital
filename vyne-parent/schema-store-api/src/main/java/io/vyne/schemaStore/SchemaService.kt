package io.vyne.schemaStore

import io.vyne.schemas.CompositeSchema
import io.vyne.schemas.taxi.NamedSource
import io.vyne.schemas.taxi.TaxiSchema
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import java.io.Serializable
import java.util.concurrent.atomic.AtomicReference

typealias SchemaSetId = Int

data class SchemaSet(val sources: List<VersionedSchema>, val generation: Int) : Serializable {
   val id: Int = sources.hashCode()

   // The backing fields and accessors here are to avoid
   // having to serailize attributes into the cache (and make the entire
   // taxi stack serializable).
   // However, after deserialization, transient fields are not set, so we need
   // to reinit before read.
   @Transient
   private var _taxiSchemas: List<TaxiSchema>? = null
   @Transient
   private var _rawSchemaStrings: List<String>? = null
   @Transient
   private var _compositeSchema: CompositeSchema? = null

   val taxiSchemas: List<TaxiSchema>
      get() {
         if (this._taxiSchemas == null) {
            init()
         }
         return this._taxiSchemas ?: error("SchemaSet failed to initialize")
      }

   val rawSchemaStrings: List<String>
      get() {
         if (this._rawSchemaStrings == null) {
            init()
         }
         return this._rawSchemaStrings ?: error("SchemaSet failed to initialize")
      }

   val schema: CompositeSchema
      get() {
         if (this._compositeSchema == null) {
            init()
         }
         return this._compositeSchema ?: error("SchemaSet failed to initialize")
      }

   private fun init() {
      val namedSources = this.sources.map { it.namedSource }
      this._taxiSchemas = TaxiSchema.from(namedSources)
      this._rawSchemaStrings = this.sources.map { it.content }
      this._compositeSchema = CompositeSchema(this._taxiSchemas!!)
   }

   companion object {
      val EMPTY = SchemaSet(emptyList(), -1)

      fun just(src: String): SchemaSet {
         return SchemaSet(listOf(VersionedSchema("Unnamed", "1.0.0", src)), -1)
      }
   }

   fun size() = sources.size

   fun add(schema: VersionedSchema): SchemaSet {
      return SchemaSet(this.sources + schema, this.generation + 1)
   }

   override fun toString(): String {
      return "SchemaSet on Generation $generation with id $id and ${this.size()} schemas"
   }
}

typealias SchemaId = String

data class VersionedSchema(val name: String, val version: String, val content: String) : Serializable {
   val id: SchemaId = "$name:$version"

   val namedSource = NamedSource(content, id)
}

@RequestMapping("/schemas/taxi")
@FeignClient(name = "\${vyne.schemaStore.name}")
interface SchemaService {

   @RequestMapping(method = arrayOf(RequestMethod.POST), value = ["/{schemaId}/{version}"])
   fun submitSchema(@RequestBody schema: String, @PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String): VersionedSchema

   @RequestMapping(method = arrayOf(RequestMethod.DELETE), value = ["/{schemaId}/{version}"])
   fun removeSchema(@PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String)

   @RequestMapping(method = arrayOf(RequestMethod.GET))
   fun listSchemas(): SchemaSet

}

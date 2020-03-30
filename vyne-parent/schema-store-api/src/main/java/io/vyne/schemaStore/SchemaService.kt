package io.vyne.schemaStore

import com.github.zafarkhaja.semver.Version
import io.vyne.CompositeSchemaBuilder
import io.vyne.NamedSource
import io.vyne.VersionedSource
import io.vyne.schemas.CompositeSchema
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.log
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import java.io.Serializable
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

typealias SchemaSetId = Int

data class SchemaSet private constructor(val sources: List<VersionedSource>, val generation: Int) : Serializable {
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
      if (this.sources.isEmpty()) {
         this._taxiSchemas = emptyList()
         this._rawSchemaStrings = emptyList()
         this._compositeSchema = CompositeSchema(emptyList())
      } else {
         // TODO : Partway through simplifying everything to have a single schema.
         // Not sure what the impact of changing this is, so will chicken out and defer
         this._taxiSchemas = listOf(TaxiSchema.from(sources))
         this._rawSchemaStrings = this.sources.map { it.content }
         this._compositeSchema = CompositeSchema(this._taxiSchemas!!)
      }
   }

   companion object {
      val EMPTY = SchemaSet(emptyList(), -1)

      fun just(src: String): SchemaSet {
         return SchemaSet(listOf(VersionedSource.sourceOnly(src)), -1)
      }

      fun from(sources: List<VersionedSource>, generation: Int): SchemaSet {
         val byName = sources.groupBy { it.name }
         val latestVersionsOfSources = byName.map { (_,candidates) ->
            candidates.maxBy { it.semver }!!
         }
         return SchemaSet(latestVersionsOfSources, generation)
      }
   }

   fun size() = sources.size

   fun contains(name: String, version: String): Boolean {
      return this.sources.any { it.name == name && it.version == version }
   }

   fun add(schemas: List<VersionedSource>): SchemaSet {
      return schemas.fold(this) { schemaSet, versionedSource -> schemaSet.add(versionedSource) }
   }

   fun add(schema: VersionedSource): SchemaSet {
      val existingSchema = this.sources.firstOrNull { it.name == schema.name }
      val sourcesToUse = if (existingSchema != null) {
         if (existingSchema.semver >= schema.semver) {
            log().info("When adding ${schema.id} version ${existingSchema.id} was found, so not making any changes")
            return this
         } else {
            log().info("Replacing ${existingSchema.id} with ${schema.id}")
            this.sources.subtract(listOf(existingSchema)).toList()
         }
      } else {
         this.sources
      }
      return SchemaSet(sourcesToUse + schema, this.generation + 1)
   }

   override fun toString(): String {
      return "SchemaSet on Generation $generation with id $id and ${this.size()} schemas"
   }
}

@RequestMapping("/schemas/taxi")
@FeignClient(name = "\${vyne.schemaStore.name}")
interface SchemaService {

   @RequestMapping(method = arrayOf(RequestMethod.POST), value = ["/{schemaId}/{version}"])
   fun submitSchema(@RequestBody schema: String, @PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String): VersionedSource

   @RequestMapping(method = arrayOf(RequestMethod.DELETE), value = ["/{schemaId}/{version}"])
   fun removeSchema(@PathVariable("schemaId") schemaId: String, @PathVariable("version") version: String)

   @RequestMapping(method = arrayOf(RequestMethod.GET))
   fun listSchemas(): SchemaSet

}

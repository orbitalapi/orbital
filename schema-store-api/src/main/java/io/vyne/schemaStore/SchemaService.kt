package io.vyne.schemaStore

import io.vyne.ParsedSource
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

typealias SchemaSetId = Int

data class SchemaSet private constructor(val sources: List<ParsedSource>, val generation: Int) : Serializable {
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

   val validSources = sources.filter { it.isValid }.map { it.source }
   val invalidSources = sources.filter { !it.isValid }.map { it.source }
   val allSources = sources.map { it.source }
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
         this._taxiSchemas = listOf(TaxiSchema.from(validSources))
         this._rawSchemaStrings = this.validSources.map { it.content }
         this._compositeSchema = CompositeSchema(this._taxiSchemas!!)
      }
   }

   companion object {
      val EMPTY = SchemaSet(emptyList(), -1)

      @Deprecated("call fromParsed instead")
      fun just(src: String): SchemaSet {
         return from(listOf(VersionedSource.sourceOnly(src)), -1)
      }

      fun fromParsed(sources: List<ParsedSource>, generation: Int): SchemaSet {
         val byName = sources.groupBy { it.name }
         val latestVersionsOfSources = byName.map { (_, candidates) ->
            candidates.maxBy { it.source.semver }!!
         }
         return SchemaSet(latestVersionsOfSources, generation)
      }

      @Deprecated("call fromParsed instead")
      fun from(sources: List<VersionedSource>, generation: Int): SchemaSet {
         log().warn("Creating a schemaSet without parsing content first can lead to unexpected results")
         return fromParsed(sources.map { ParsedSource(it, emptyList()) }, generation)
      }

   }

   fun size() = sources.size

   fun contains(name: String, version: String): Boolean {
      return this.sources.any { it.source.name == name && it.source.version == version }
   }

   fun offerSource(source:VersionedSource): List<VersionedSource>  {
      return this.allSources.addIfNewer(source)
   }

   fun offerSources(sources: List<VersionedSource>): List<VersionedSource> {
      return sources.fold(this.allSources) { acc,source -> acc.addIfNewer(source)  }
   }


   override fun toString(): String {
      val invalidSchemaSuffix = if (invalidSources.isNotEmpty()) {
         ", ${invalidSources.size} of which have errors"
      } else {
         ""
      }
      return "SchemaSet on Generation $generation with id $id and ${this.size()} schemas$invalidSchemaSuffix"
   }

   private fun List<VersionedSource>.addIfNewer(source:VersionedSource):List<VersionedSource> {
      val existingSource = this.firstOrNull { it.name == source.name }
      return if (existingSource != null) {
         if (existingSource.semver >= source.semver) {
            log().info("When adding ${source.id} version ${existingSource.id} was found, so not making any changes")
            return this
         } else {
            log().info("Replacing ${existingSource.id} with ${source.id}")
            this.subtract(listOf(existingSource)).toList() + source
         }
      } else {
         this + source
      }
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

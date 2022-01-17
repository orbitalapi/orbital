package io.vyne.schemaApi

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.ParsedSource
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schemas.CompositeSchema
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.log
import java.io.Serializable

data class SchemaSet private constructor(val sources: List<ParsedSource>, val generation: Int) : Serializable {
   val id: Int = sources.hashCode()

   init {
      log().info("SchemaSet with generation $generation created")
   }

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

   @get:JsonIgnore
   val validSources = sources.filter { it.isValid }.map { it.source }

   @get:JsonIgnore
   val invalidSources = sources.filter { !it.isValid }.map { it.source }

   @get:JsonIgnore
   val allSources = sources.map { it.source }

   @get:JsonIgnore
   val taxiSchemas: List<TaxiSchema>
      get() {
         if (this._taxiSchemas == null) {
            init()
         }
         return this._taxiSchemas ?: error("SchemaSet failed to initialize")
      }

   @get:JsonIgnore
   val rawSchemaStrings: List<String>
      get() {
         if (this._rawSchemaStrings == null) {
            init()
         }
         return this._rawSchemaStrings ?: error("SchemaSet failed to initialize")
      }

   @get:JsonIgnore
   val schema: CompositeSchema
      get() {
         if (this._compositeSchema == null) {
            init()
         }
         return this._compositeSchema ?: error("SchemaSet failed to initialize")
      }

   private fun init() {
      log().info("Initializing schema set with generation $generation")
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

   fun offerSource(source: VersionedSource): List<VersionedSource> {
      return this.allSources.addIfNewer(source)
   }

   /**
    * Evaluates the set of offered sources, and returns a merged set
    * containing the latest of all schemas (as determined using their semantic version)
    */
   fun offerSources(sources: List<VersionedSource>, sourcesTobeRemoved: List<SchemaId> = emptyList()): List<VersionedSource> {
      return if (sourcesTobeRemoved.isEmpty()) {
         sources.fold(this.allSources) { acc, source -> acc.addIfNewer(source) }
      } else {
         sources.fold(this.allSources) { acc, source -> acc.addIfNewer(source) }
         this.removeSources(sourcesTobeRemoved)
      }
   }

   fun removeSources(sourcesTobeRemoved: List<SchemaId>): List<VersionedSource> {
      return this.allSources.filter {  !sourcesTobeRemoved.contains(it.id) }
   }


   override fun toString(): String {
      val invalidSchemaSuffix = if (invalidSources.isNotEmpty()) {
         ", ${invalidSources.size} of which have errors"
      } else {
         ""
      }
      return "SchemaSet on Generation $generation with id $id and ${this.size()} schemas$invalidSchemaSuffix"
   }

   private fun List<VersionedSource>.addIfNewer(source: VersionedSource): List<VersionedSource> {
      val existingSource = this.firstOrNull { it.name == source.name }
      return if (existingSource != null) {
         if (existingSource.semver.compareWithBuildsTo(source.semver) > 0) {
            log().info("When adding ${source.id} version ${existingSource.id} was found, so not making any changes")
            return this
         }
         else {
            this.subtract(listOf(existingSource)).toList() + source
         }
      } else {
         this + source
      }
   }
}

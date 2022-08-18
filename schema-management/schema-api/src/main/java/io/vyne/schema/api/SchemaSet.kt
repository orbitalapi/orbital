package io.vyne.schema.api

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.PackageIdentifier
import io.vyne.ParsedPackage
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schemas.CompositeSchema
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.log
import mu.KotlinLogging
import java.io.Serializable

data class SchemaSet private constructor(val parsedPackages: List<ParsedPackage>, val generation: Int) : Serializable {
   val id: Int = parsedPackages.hashCode()

   private val packagesById = parsedPackages.associateBy { it.identifier.unversionedId }

   init {
      log().info("SchemaSet with generation $generation created")
      val byGroupId = parsedPackages.groupBy { it.identifier.unversionedId }
         .filter { (_,v) -> v.size > 1 }

      if (byGroupId.isNotEmpty()) {
         log().warn("The following packages appear to have mutliple versions in the same schema set: ${byGroupId.keys.joinToString(", ")}")

      }


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
   val validPackages = parsedPackages.filter { it.isValid }

   @get:JsonIgnore
   val invalidPackages = parsedPackages.filter { !it.isValid }

   @get:JsonIgnore
   val validSources = validPackages.filter { it.isValid }.flatMap { it.sources }.map { it.source }

   @get:JsonIgnore
   val sourcesWithErrors = parsedPackages.filter { !it.isValid }.flatMap { it.sourcesWithErrors }

   @get:JsonIgnore
   val allSources = parsedPackages.flatMap { sourcePackage -> sourcePackage.sources.map { it.source } }

   @get:JsonIgnore
   val packages: List<SourcePackage>
      get() {
         return this.parsedPackages.map { it.toSourcePackage() }
      }

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
      if (this.parsedPackages.isEmpty()) {
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
      private val logger = KotlinLogging.logger {}

      @Deprecated("call fromParsed instead")
      fun just(src: String): SchemaSet {
         return from(listOf(VersionedSource.sourceOnly(src)), -1)
      }

      fun fromParsed(sources: List<ParsedPackage>, generation: Int): SchemaSet {
         return SchemaSet(sources, generation)
      }

      @Deprecated("call fromParsed instead")
      fun from(sources: List<VersionedSource>, generation: Int): SchemaSet {
         error("Creating a SchemaSet from raw sources is not supported - pass a package instead.")
//         log().warn("Creating a schemaSet without parsing content first can lead to unexpected results")
//         return fromParsed(sources.map { ParsedSource(it, emptyList()) }, generation)
      }

      fun from(schema: Schema, generation: Int): SchemaSet {
         return from(schema.sources, generation)
      }

   }

   fun size() = parsedPackages.size


   /**
    * Returns a set of SourcePackages that exist
    * after applying the update.
    *
    * This SchemaSet is not changed.
    */
   fun getPackagesAfterUpdate(
      sourcePackage: SourcePackage? = null,
      packagesToBeRemoved: List<PackageIdentifier> = emptyList()
   ): List<SourcePackage> {
      val allPackages = this.parsedPackages.associateBy { it.identifier }
         .mapValues { (_, parsed) -> parsed.toSourcePackage() }
         .toMutableMap()
      packagesToBeRemoved.forEach {
         val removed = allPackages.remove(it)
         if (removed != null) {
            logger.info { "Removed ${removed.identifier}" }
         }
      }

      if (sourcePackage != null) {
         // Find packages that match on the identifier, excluding the version (since an updated version has been proposed)
         val existingEntries = allPackages.filterKeys { it.unversionedId == sourcePackage.identifier.unversionedId }
         if (existingEntries.isNotEmpty()) {
            logger.info { "Replacing ${existingEntries.keys.joinToString { it.id }} with ${sourcePackage.packageMetadata.identifier}" }
         }
         existingEntries.keys.forEach { allPackages.remove(it) }
         allPackages[sourcePackage.identifier] = sourcePackage
      }
      return allPackages.values.toList()
   }

   override fun toString(): String {
      val invalidSchemaSuffix = if (sourcesWithErrors.isNotEmpty()) {
         ", ${sourcesWithErrors.size} of which have errors"
      } else {
         ""
      }
      return "SchemaSet on Generation $generation with id $id and ${this.size()} packages$invalidSchemaSuffix"
   }


}

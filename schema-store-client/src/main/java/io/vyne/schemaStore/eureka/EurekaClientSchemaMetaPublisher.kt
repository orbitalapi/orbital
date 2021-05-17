package io.vyne.schemaStore.eureka

import arrow.core.Either
import com.netflix.appinfo.ApplicationInfoManager
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schemaStore.SchemaPublisher
import io.vyne.schemaStore.eureka.EurekaMetadata.isVyneMetadata
import io.vyne.schemas.Schema
import io.vyne.schemas.SimpleSchema
import io.vyne.utils.log
import lang.taxi.CompilationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class EurekaClientSchemaMetaPublisher(
   private val applicationInfoManager: ApplicationInfoManager,
   @Value("\${vyne.taxi.rest.path:/taxi}") private val taxiRestPath: String,
   @Value("\${server.servlet.context-path:}") private val contextPath: String
) : SchemaPublisher {
   private var sources: List<VersionedSource> = emptyList()
   override fun submitSchemas(versionedSources: List<VersionedSource>, removedSource: List<SchemaId>): Either<CompilationException, Schema> {
      val servletContextTaxiPath = contextPath + taxiRestPath
      log().info("Registering schema at endpoint $servletContextTaxiPath")
      val schemaMetadata = versionedSources.map { versionedSource ->
         EurekaMetadata.escapeForXML("${EurekaMetadata.VYNE_SOURCE_PREFIX}${versionedSource.id}") to versionedSource.contentHash }
         .toMap() +
         mapOf(EurekaMetadata.VYNE_SCHEMA_URL to servletContextTaxiPath)
      registerEurekaMetadata(schemaMetadata)
      this.sources = versionedSources

      // this return type doesn't feel right - we don't actually know the
      // schema.  Originally the idea was that by publishing a schema you'd get
      // to know the full federated schema.  However, feedback is that service developers
      // don't really want their services to become burdened with this.
      // which makes sense, ie., apps likely don't need to know / care.
      return Either.right(SimpleSchema.EMPTY)
   }

   private fun registerEurekaMetadata(latestMetadata: Map<String, String>) {
      // Note that we're not using:
      // applicationInfoManager.registerAppMetadata(latestMetadata)
      // as above call 'appends latestMetadata' into the existing application metadata, rather than replacing it.

      // Clear the existing vyne related metadata.
      applicationInfoManager.info.metadata.entries.removeAll { (key, _) -> isVyneMetadata(key)  }
      // and set the metadata
      applicationInfoManager.registerAppMetadata(latestMetadata)

   }

   @GetMapping("\${vyne.taxi.rest.path:/taxi}") // TODO : Make configurable
   fun getTaxiSources(): List<VersionedSource> {
      return sources
   }
}


package io.vyne.schemaStore.eureka

import arrow.core.Either
import arrow.core.right
import com.netflix.appinfo.ApplicationInfoManager
import io.vyne.VersionedSource
import io.vyne.schemaStore.SchemaPublisher
import io.vyne.schemas.Schema
import io.vyne.schemas.SimpleSchema
import lang.taxi.CompilationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class EurekaClientSchemaMetaPublisher(
   private val applicationInfoManager: ApplicationInfoManager,
   @Value("\${vyne.taxi.rest.path:/taxi}") private val taxiRestPath: String) : SchemaPublisher {
   private var sources: List<VersionedSource> = emptyList()
   override fun submitSchemas(versionedSources: List<VersionedSource>): Either<CompilationException, Schema> {
      val schemaMetadata = versionedSources.map { versionedSource -> "${EurekaMetadata.VYNE_SOURCE_PREFIX}${versionedSource.id}" to versionedSource.contentHash }
         .toMap() +
         mapOf(EurekaMetadata.VYNE_SCHEMA_URL to taxiRestPath)
      applicationInfoManager.registerAppMetadata(schemaMetadata)
      this.sources = versionedSources

      // this return type doesn't feel right - we don't actually know the
      // schema.  Originally the idea was that by publishing a schema you'd get
      // to know the full federated schema.  However, feedback is that service developers
      // don't really want their services to become burdened with this.
      // which makes sense, ie., apps likely don't need to know / care.
      return Either.right(SimpleSchema(emptySet(), emptySet()))
   }

   @GetMapping("\${vyne.taxi.rest.path:/taxi}") // TODO : Make configurable
   fun getTaxiSources(): List<VersionedSource> {
      return sources
   }
}


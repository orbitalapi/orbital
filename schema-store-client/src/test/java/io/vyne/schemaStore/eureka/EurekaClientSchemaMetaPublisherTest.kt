package io.vyne.schemaStore.eureka

import com.netflix.appinfo.ApplicationInfoManager
import com.netflix.appinfo.InstanceInfo
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import org.junit.Test

class EurekaClientSchemaMetaPublisherTest {
   private val instanceInfo = InstanceInfo
      .Builder
      .newBuilder()
      .setAppName("testApp")
      .add("metadataKey", "metadataValue")
      .add("vyne.foo", "bar")
      .build()

   private val applicationInfoManager = ApplicationInfoManager(mock(), instanceInfo)

   private val eurekaClientSchemaMetaPublisher = EurekaClientSchemaMetaPublisher(applicationInfoManager, "taxiRestPath", "contextPath")

   @Test
   fun `schema publication should not delete non-vyne metadata from eureka`() {
      eurekaClientSchemaMetaPublisher.submitSchemas(listOf())
      val existingMetadata = applicationInfoManager.info.metadata
      existingMetadata.should.equal(mapOf(
         "vyne.schema.url" to "contextPathtaxiRestPath",
         "metadataKey" to "metadataValue"
      ))
   }
}

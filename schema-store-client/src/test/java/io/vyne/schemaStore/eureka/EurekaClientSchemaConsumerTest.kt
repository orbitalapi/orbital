package io.vyne.schemaStore.eureka

import com.winterbe.expekt.should
import io.vyne.VersionedSource
import org.junit.Test

class EurekaClientSchemaConsumerTest {
   @Test
   fun `source names with hyphen`() {
      val versionedSource = VersionedSource(name = "product-service", version = "0.0.1", content = "model Foo { foo: String }")
      val schemaMetadata = EurekaMetadata.escapeForXML("${EurekaMetadata.VYNE_SOURCE_PREFIX}${versionedSource.id}")
      val original = EurekaMetadata.fromXML(schemaMetadata)
      versionedSource.id.should.be.equal(original.replace(EurekaMetadata.VYNE_SOURCE_PREFIX, ""))
   }
}

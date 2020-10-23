package io.vyne.schemaStore

import com.winterbe.expekt.should
import io.vyne.VersionedSource
import org.junit.Test

class LocalValidatingSchemaStoreClientTest {
   @Test
   fun `schema with Errors Test`() {
      val localValidatingSchemaStoreClient = LocalValidatingSchemaStoreClient()
      val orderVersionedSource = VersionedSource(name = "order.taxi", version = "0.0.1", content = """
         namespace foo.bar {
             model Order {
                 orderId: String
              }
         }
      """.trimIndent())

      val orderServiceVersionedSource = VersionedSource(name = "order-service.taxi", version = "0.0.1", content = """
         import foo.bar.Order
         namespace foo.service {
             service OrderService {
                 operation findAll(): Order[]
              }
         }
      """.trimIndent())

      localValidatingSchemaStoreClient.submitSchemas(listOf(orderVersionedSource, orderServiceVersionedSource))
      val schema = localValidatingSchemaStoreClient.schemaSet().schema
      schema.hasType("foo.bar.Order").should.be.`true`

      val updatedOrderSource = VersionedSource(name = "order.taxi", version = "0.0.2", content = """
         namespace foo.bar {
             model OrderEx {
                 orderId: String
              }
         }
      """.trimIndent())
      localValidatingSchemaStoreClient.submitSchema(updatedOrderSource)
      val latestSchema = localValidatingSchemaStoreClient.schemaSet().schema
      latestSchema.services.should.be.empty
   }
}

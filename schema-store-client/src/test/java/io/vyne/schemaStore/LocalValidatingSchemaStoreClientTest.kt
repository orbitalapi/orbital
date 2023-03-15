package io.vyne.schemaStore

import com.winterbe.expekt.should
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import org.junit.Test

class LocalValidatingSchemaStoreClientTest {

   @Test
   fun `resubmitting the same schema has no effect`() {
      val localValidatingSchemaStoreClient = LocalValidatingSchemaStoreClient()
      val ordersPackageV1 = SourcePackage(
         PackageMetadata.from("com.foo", "Orders", "0.1.0"),
         sources = listOf(
            VersionedSource(
               name = "order.taxi", version = "0.0.1", content = """
         namespace foo.bar {
             model Order {
                 orderId: String
              }
         }
      """.trimIndent()
            )
         )
      )
      localValidatingSchemaStoreClient.submitPackage(ordersPackageV1)
      val result = localValidatingSchemaStoreClient.submitPackage(ordersPackageV1)
      val schema = result.orNull()!!
      schema.hasType("Order").should.be.`true`
   }

   @Test
   fun `updating a package removes previous sources`() {
      val localValidatingSchemaStoreClient = LocalValidatingSchemaStoreClient()
      val ordersPackageV1 = SourcePackage(
         PackageMetadata.from("com.foo", "Orders", "0.1.0"),
         sources = listOf(
            VersionedSource(
               name = "order.taxi", version = "0.0.1", content = """
         namespace foo.bar {
             model Order {
                 orderId: String
              }
         }
      """.trimIndent()
            )
         )
      )


      localValidatingSchemaStoreClient.submitPackage(ordersPackageV1)
      val schema = localValidatingSchemaStoreClient.schemaSet.schema
      schema.hasType("foo.bar.Order").should.be.`true`

      val ordersPackageV2 = ordersPackageV1.copy(
         sources = listOf(
            VersionedSource(
               name = "order.taxi", version = "0.0.2", content = """
         namespace foo.bar {
             model OrderEx {
                 orderId: String
              }
         }
      """.trimIndent()
            )
         )
      )
      localValidatingSchemaStoreClient.submitPackage(ordersPackageV2)
      val latestSchema = localValidatingSchemaStoreClient.schemaSet.schema
      latestSchema.hasType("foo.bar.OrderEx").should.be.`true`
      latestSchema.hasType("foo.bar.Order").should.be.`false`
   }
}

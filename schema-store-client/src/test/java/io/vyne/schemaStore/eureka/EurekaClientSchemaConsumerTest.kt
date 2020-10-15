package io.vyne.schemaStore.eureka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.util.concurrent.MoreExecutors
import com.netflix.appinfo.InstanceInfo
import com.netflix.discovery.CacheRefreshedEvent
import com.netflix.discovery.EurekaClient
import com.netflix.discovery.EurekaEventListener
import com.netflix.discovery.shared.Application
import com.netflix.discovery.shared.Applications
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.VersionedSource
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import org.junit.Before
import org.junit.Test
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.client.RestTemplate
import javax.inject.Provider

class EurekaClientSchemaConsumerTest {
   val mockEurekaClientProvider = mock<Provider<EurekaClient>>()
   private val mockLocalValidatingSchemaStoreClient = LocalValidatingSchemaStoreClient()
   val mockApplicationEventPublisher = mock< ApplicationEventPublisher>()
   val mockEurekaClient = mock<EurekaClient>()
   private val eurekaApplications = Applications()
   @Before
   fun setUp() {
      whenever(mockEurekaClientProvider.get()).thenReturn(mockEurekaClient)
      whenever(mockEurekaClient.applications).thenReturn(eurekaApplications)
   }

   @Test
   fun `source names with hyphen`() {
      val versionedSource = VersionedSource(name = "product-service", version = "0.0.1", content = "model Foo { foo: String }")
      val schemaMetadata = EurekaMetadata.escapeForXML("${EurekaMetadata.VYNE_SOURCE_PREFIX}${versionedSource.id}")
      val original = EurekaMetadata.fromXML(schemaMetadata)
      versionedSource.id.should.be.equal(original.replace(EurekaMetadata.VYNE_SOURCE_PREFIX, ""))
   }

   @Test
   fun `can detect removed sources and update whole schema accordingly`() {
      // Given
      val instanceInfo = instanceInfo("file-schema-server", mapOf("vyne.sources.product.taxi___0.0.1" to "47df0b", "vyne.sources.order.taxi___0.0.1" to "12321"))
      val productVersionedSource = VersionedSource(name = "product.taxi", version = "0.0.1", content = """
         namespace foo.bar {
             model Product {
                 id: String
              }
         }
      """.trimIndent())
      val orderVersionedSource = VersionedSource(name = "order.taxi", version = "0.0.1", content = """
         namespace foo.bar {
             model Order {
                 orderId: String
              }
         }
      """.trimIndent())
      val (eurekaEventListener, server, eurekaClientSchemaConsumer) = initialise(listOf(instanceInfo),
         listOf(Pair(instanceInfo, listOf(productVersionedSource, orderVersionedSource))))

      // When
      eurekaEventListener!!.onEvent(CacheRefreshedEvent())
      eurekaClientSchemaConsumer.schemaSet().taxiSchemas.size.should.equal(1)
      eurekaClientSchemaConsumer.schemaSet().taxiSchemas.first().sources.size.should.equal(2)
      val orderSource = eurekaClientSchemaConsumer.schemaSet().taxiSchemas.first().sources.first {it.name == "order.taxi"}

      // One of the files deleted
      instanceInfo.metadata.remove("vyne.sources.product.taxi___0.0.1") // remove one of the files.
      setTaxiSchemasRestResponse(server, listOf(Pair(instanceInfo, listOf(orderVersionedSource))))
      // When
      eurekaEventListener!!.onEvent(CacheRefreshedEvent())
      eurekaClientSchemaConsumer.schemaSet().taxiSchemas.size.should.equal(1)
      eurekaClientSchemaConsumer.schemaSet().taxiSchemas.first().sources.size.should.equal(1)
      val remainingSource = eurekaClientSchemaConsumer.schemaSet().taxiSchemas.first().sources.first()
      remainingSource.content.should.equal(orderVersionedSource.content)
      (remainingSource.name == orderSource.name).should.be.`true`

   }

   @Test
   fun `can update content of an existing file even if the version not incremented`() {
      // Given
      val instanceInfo = instanceInfo("file-schema-server", mapOf("vyne.sources.product.taxi___0.0.1" to "47df0b"))
      val productVersionedSource = VersionedSource(name = "product.taxi", version = "0.0.1", content = """
         namespace foo.bar {
             model Product {
                 id: String
              }
         }
      """.trimIndent())
      val (eurekaEventListener, server, eurekaClientSchemaConsumer) = initialise(listOf(instanceInfo),
         listOf(Pair(instanceInfo, listOf(productVersionedSource))))
      // When
      eurekaEventListener!!.onEvent(CacheRefreshedEvent())
      eurekaClientSchemaConsumer.schemaSet().taxiSchemas.size.should.equal(1)
      eurekaClientSchemaConsumer.schemaSet().taxiSchemas.first().sources.size.should.equal(1)

      // Update content
      val updatedProductVersionedSource = VersionedSource(name = "product.taxi", version = "0.0.1", content = """
         namespace foo.bar {
             model Product {
                 id: String
                 issueDate: Date
              }
         }
      """.trimIndent())

      setTaxiSchemasRestResponse(server, listOf(Pair(instanceInfo, listOf(updatedProductVersionedSource))))
      instanceInfo.metadata["vyne.sources.product.taxi___0.0.1"] = "2caef"
      // When
      eurekaEventListener!!.onEvent(CacheRefreshedEvent())
      eurekaClientSchemaConsumer.schemaSet().taxiSchemas.size.should.equal(1)
      eurekaClientSchemaConsumer.schemaSet().taxiSchemas.first().sources.size.should.equal(1)
      eurekaClientSchemaConsumer.schemaSet().taxiSchemas.first().sources.first().content.should.equal(updatedProductVersionedSource.content)

   }

   @Test
   fun `Handling of multiple schema sources`() {
      val instanceInfo1 = instanceInfo("file-schema-server", mapOf("vyne.sources.product.taxi___0.0.1" to "47df0b"))
      val productVersionedSource = VersionedSource(name = "product.taxi", version = "0.0.1", content = """
         namespace foo.bar {
             model Product {
                 id: String
              }
         }
      """.trimIndent())

      val instanceInfo2 = instanceInfo("order-schema-provider", mapOf("vyne.sources.order.taxi___0.0.1" to "12321"), 12345)
      val orderVersionedSource = VersionedSource(name = "order.taxi", version = "0.0.1", content = """
         namespace foo.bar {
             model Order {
                 orderId: String
              }
         }
      """.trimIndent())

      val (eurekaEventListener, server, eurekaClientSchemaConsumer) = initialise(listOf(instanceInfo1, instanceInfo2),
         listOf(Pair(instanceInfo1, listOf(productVersionedSource)), Pair(instanceInfo2, listOf(orderVersionedSource))))

      // When
      eurekaEventListener!!.onEvent(CacheRefreshedEvent())
      eurekaClientSchemaConsumer.schemaSet().taxiSchemas.size.should.equal(1)
      eurekaClientSchemaConsumer.schemaSet().taxiSchemas.first().sources.size.should.equal(2)

      // update version
      val updatedProductVersionedSource = productVersionedSource.copy(version = "0.0.2")
      instanceInfo1.metadata.remove("vyne.sources.product.taxi___0.0.1")
      instanceInfo1.metadata["vyne.sources.product.taxi___0.0.2"] = "47df0b"
      setTaxiSchemasRestResponse(server, listOf(Pair(instanceInfo1, listOf(updatedProductVersionedSource))))
      // Whens
      eurekaEventListener!!.onEvent(CacheRefreshedEvent())
      eurekaClientSchemaConsumer.schemaSet().taxiSchemas.size.should.equal(1)
      eurekaClientSchemaConsumer.schemaSet().taxiSchemas.first().sources.size.should.equal(2)
      eurekaClientSchemaConsumer.schemaSet().taxiSchemas.first().sources.map { it.version }.should.contain("0.0.2")
   }

   @Test
   fun `can handle failure whilst retrieving taxi files from a registered application`() {
      // Given
      val instanceInfo = instanceInfo("file-schema-server", mapOf("vyne.sources.product.taxi___0.0.1" to "47df0b", "vyne.sources.order.taxi___0.0.1" to "12321"))
      val productVersionedSource = VersionedSource(name = "product.taxi", version = "0.0.1", content = """
         namespace foo.bar {
             model Product {
                 id: String
              }
         }
      """.trimIndent())
      val orderVersionedSource = VersionedSource(name = "order.taxi", version = "0.0.1", content = """
         namespace foo.bar {
             model Order {
                 orderId: String
              }
         }
      """.trimIndent())
      val (eurekaEventListener, server, eurekaClientSchemaConsumer) = initialise(listOf(instanceInfo),
         listOf(Pair(instanceInfo, listOf())))

      // When
      eurekaEventListener!!.onEvent(CacheRefreshedEvent())
      eurekaClientSchemaConsumer.schemaSet().taxiSchemas.should.be.empty

      // One of the files deleted
      //instanceInfo.metadata.remove("vyne.sources.product.taxi___0.0.1") // remove one of the files.
      setTaxiSchemasRestResponse(server, listOf(Pair(instanceInfo, listOf(productVersionedSource, orderVersionedSource))))
      // When
      eurekaEventListener!!.onEvent(CacheRefreshedEvent())
      eurekaClientSchemaConsumer.schemaSet().taxiSchemas.size.should.equal(1)
      eurekaClientSchemaConsumer.schemaSet().taxiSchemas.first().sources.size.should.equal(2)
   }

   private fun instanceInfo(appName: String, sourceMap: Map<String, String>, port: Int = 1234) = InstanceInfo
      .Builder
      .newBuilder()
      .setInstanceId("instanceId")
      .setAppName(appName)
      .setHostName("localhost")
      .setPort(port)
      .setMetadata(mutableMapOf(EurekaMetadata.VYNE_SCHEMA_URL to "/taxi").plus(sourceMap).toMutableMap())
      .build()

   private fun initialise(instanceInfos: List<InstanceInfo>,
                          initialVersionedSources: List<Pair<InstanceInfo, List<VersionedSource>>>): Triple<EurekaEventListener, MockRestServiceServer, EurekaClientSchemaConsumer> {
      instanceInfos.forEach { instanceInfo ->
         val application = Application(instanceInfo.appName, listOf(instanceInfo))
         eurekaApplications.addApplication(application)
      }

      val restTemplate = RestTemplate()
      val server = MockRestServiceServer.bindTo(restTemplate).build()
      setTaxiSchemasRestResponse(server, initialVersionedSources)
      var eurekaEventListener: EurekaEventListener? = null
      whenever(mockEurekaClient.registerEventListener(any())).then { invocationOnMock ->
         eurekaEventListener = invocationOnMock.getArgument(0) as EurekaEventListener?
         invocationOnMock.getArgument(0)
      }
      val eurekaClientSchemaConsumer = EurekaClientSchemaConsumer(
         mockEurekaClientProvider,
         mockLocalValidatingSchemaStoreClient,
         mockApplicationEventPublisher,
         restTemplate,
         MoreExecutors.newDirectExecutorService())

      return Triple(eurekaEventListener!!, server, eurekaClientSchemaConsumer)
   }


   private fun setTaxiSchemasRestResponse(server: MockRestServiceServer, responses: List<Pair<InstanceInfo, List<VersionedSource>>>) {
      server.reset()
      responses.forEach { (instanceInfo, response) ->
         if (response.isEmpty()) {
            server.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo("http://${instanceInfo.hostName}:${instanceInfo.port}/taxi"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andExpect(MockRestRequestMatchers.anything())
               .andRespond(MockRestResponseCreators.withServerError())
         } else {
            server.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo("http://${instanceInfo.hostName}:${instanceInfo.port}/taxi"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andExpect(MockRestRequestMatchers.anything())
               .andRespond(MockRestResponseCreators.withSuccess(jacksonObjectMapper().writeValueAsString(response), MediaType.APPLICATION_JSON))
         }
      }

   }
}

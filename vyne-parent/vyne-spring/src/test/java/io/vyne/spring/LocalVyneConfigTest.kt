package io.vyne.spring

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.expect
import io.vyne.schemaStore.HttpSchemaStoreClient
import io.vyne.schemaStore.SchemaService
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemaStore.VersionedSchema
import lang.taxi.annotations.DataType
import lang.taxi.annotations.Service
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(classes = arrayOf(PropertyConfig::class, LocalVyneConfigTest.vyneConfigWithLocalSchemaStore::class))
@TestPropertySource(properties = arrayOf("vyne.schema.name=testSchema", "vyne.schema.version=0.1.0"))
class LocalVyneConfigTest {

   @Autowired
   internal var vyneFactory: VyneFactory? = null

   @MockBean
   internal var schemaService: SchemaService? = null

   @Test
   fun parsesContextCorrectly() {
      val vyne = vyneFactory!!.`object`
      expect(vyne).not.`null`
      expect(vyne.schema.services).to.have.size(1)
      expect(vyne.schema.types).to.have.size(1)
   }


   @VyneSchemaPublisher(publicationMethod = SchemaPublicationMethod.DISABLED)
   @Import(VyneAutoConfiguration::class)
   class vyneConfigWithLocalSchemaStore {}
}

@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(classes = arrayOf(PropertyConfig::class, RemoteVyneConfigTest.vyneConfigWithRemoteSchemaStore::class))
@TestPropertySource(properties = arrayOf("vyne.schema.name=testSchema", "vyne.schema.version=0.1.0"))
@DirtiesContext
class RemoteVyneConfigTest {

   @Autowired
   lateinit var vyneFactory: VyneFactory

   @MockBean
   lateinit var schemaService: SchemaService

   @Autowired
   lateinit var schemaStoreClient: HttpSchemaStoreClient


   @Test
   fun given_noSchemasFromRemoteService_then_vyneIsEmpty() {
      val vyne = vyneFactory.createVyne()
      expect(vyne).not.`null`
      expect(vyne.schema.services).to.be.empty
      expect(vyne.schema.types).to.be.empty
   }

   @Test
   fun given_schemaServiceReturnsSchemas_then_theyArePresentInvyne() {
      val schemaSet = SchemaSet(listOf(VersionedSchema("RemoteSchema", "0.1.0", "type MyClient {}")))
      whenever(schemaService.listSchemas()).thenReturn(schemaSet)
      schemaStoreClient.pollForSchemaUpdates()

      val vyne = vyneFactory.createVyne()
      expect(vyne).not.`null`
      expect(vyne.schema.types).to.have.size(1)
      expect(vyne.schema.type("MyClient")).not.to.be.`null`
   }

   @Test
   fun shouldPublishLocalSchemaOnStartup() {
      verify(schemaService).submitSchema(any(), eq("testSchema"), eq("0.1.0"))
   }


   @VyneSchemaPublisher(publicationMethod = SchemaPublicationMethod.REMOTE)
   @Import(VyneAutoConfiguration::class)
   class vyneConfigWithRemoteSchemaStore {}
}

@Configuration
class PropertyConfig {
   @Bean
   internal fun propertySourcesPlaceholderConfigurer(): PropertySourcesPlaceholderConfigurer {
      return PropertySourcesPlaceholderConfigurer()
   }
}


@DataType
internal class MyClient

@Service
internal class MyService

package io.polymer.spring

import com.github.zafarkhaja.semver.Version
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.expect
import io.polymer.schemaStore.SchemaService
import io.polymer.schemaStore.SchemaSet
import io.polymer.schemaStore.SchemaStoreClient
import io.polymer.schemaStore.VersionedSchema
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
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(classes = arrayOf(PropertyConfig::class, LocalPolymerConfigTest.PolymerConfigWithLocalSchemaStore::class))
@TestPropertySource(properties = arrayOf("polymer.schema.name=testSchema", "polymer.schema.version=0.1.0"))
class LocalPolymerConfigTest {

   @Autowired
   internal var polymerFactory: PolymerFactory? = null

   @MockBean
   internal var schemaService: SchemaService? = null

   @Test
   fun parsesContextCorrectly() {
      val polymer = polymerFactory!!.`object`
      expect(polymer).not.`null`
      expect(polymer.schema.services).to.have.size(1)
      expect(polymer.schema.types).to.have.size(1)
   }


   @EnablePolymer(useRemoteSchemaStore = false)
   @Import(PolymerAutoConfiguration::class)
   class PolymerConfigWithLocalSchemaStore {}
}

@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(classes = arrayOf(PropertyConfig::class, RemotePolymerConfigTest.PolymerConfigWithRemoteSchemaStore::class))
@TestPropertySource(properties = arrayOf("polymer.schema.name=testSchema", "polymer.schema.version=0.1.0"))
class RemotePolymerConfigTest {

   @Autowired
   lateinit var polymerFactory: PolymerFactory

   @MockBean
   lateinit var schemaService: SchemaService

   @Autowired
   lateinit var schemaStoreClient: SchemaStoreClient


   @Test
   fun given_noSchemasFromRemoteService_then_polymerIsEmpty() {
      val polymer = polymerFactory.createPolymer()
      expect(polymer).not.`null`
      expect(polymer.schema.services).to.be.empty
      expect(polymer.schema.types).to.be.empty
   }

   @Test
   fun given_schemaServiceReturnsSchemas_then_theyArePresentInPolymer() {
      val schemaSet = SchemaSet(listOf(VersionedSchema("RemoteSchema", Version.valueOf("0.1.0"), "type MyClient {}")))
      whenever(schemaService.listSchemas()).thenReturn(schemaSet)
      schemaStoreClient.pollForSchemaUpdates()

      val polymer = polymerFactory.createPolymer()
      expect(polymer).not.`null`
      expect(polymer.schema.types).to.have.size(1)
   }


   @EnablePolymer(useRemoteSchemaStore = true)
   @Import(PolymerAutoConfiguration::class)
   class PolymerConfigWithRemoteSchemaStore {}
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

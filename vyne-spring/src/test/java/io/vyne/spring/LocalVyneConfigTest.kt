package io.vyne.spring

import com.hazelcast.core.EntryEvent
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.listener.EntryAddedListener
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.schemas.Schema
import lang.taxi.annotations.DataType
import lang.taxi.annotations.Operation
import lang.taxi.annotations.Service
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.web.bind.annotation.GetMapping
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(classes = [PropertyConfig::class, LocalVyneConfigTest.vyneConfigWithLocalSchemaStore::class])
@TestPropertySource(properties = ["vyne.schema.name=testSchema", "vyne.schema.version=0.1.0", "spring.application.name=vyneTest"])
class LocalVyneConfigTest {

   @Autowired
   lateinit var vyneFactory: VyneFactory

   @MockBean
   internal var schemaStoreClient: SchemaStoreClient? = null

   @Test
   fun parsesContextCorrectly() {
      val vyne = vyneFactory!!.`object`
      expect(vyne).not.`null`
      expect(vyne.schema.services).to.have.size(1)

      val operations = vyne.schema.service("io.vyne.spring.MyService").operations
      expect(operations).to.have.size(1)
      expect(operations.first().metadata.first().params["url"]).equal("/getMyMethod")

      vyne.schema.types.map { it.taxiType.qualifiedName }.should.contain.all.elements(
         "lang.taxi.Boolean",
         "lang.taxi.String",
         "lang.taxi.Int",
         "lang.taxi.Decimal",
         "lang.taxi.Date",
         "lang.taxi.Time",
         "lang.taxi.DateTime",
         "lang.taxi.Instant",
         "lang.taxi.Array",
         "lang.taxi.Any",
         "lang.taxi.Double",
         "lang.taxi.Void",
         "io.vyne.spring.MyClient"
      )
   }


   @VyneSchemaPublisher(publicationMethod = SchemaPublicationMethod.DISABLED)
   @EnableAutoConfiguration
   class vyneConfigWithLocalSchemaStore {}
}

@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(classes = [PropertyConfig::class, DistributedVyneConfigTest.vyneConfigWithDistributedSchemaStore::class])
@TestPropertySource(properties = ["vyne.schema.name=testSchema", "vyne.schema.version=0.1.0", "spring.application.name=vyneTest",
"vyne.schme.publish.on.samethread=true"])
@DirtiesContext
class DistributedVyneConfigTest {

   @Autowired
   lateinit var vyneFactory: VyneFactory

   @Autowired
   lateinit var hazelcast: HazelcastInstance

   @Test
   fun given_schemaServiceReturnsSchemas_then_theyArePresentInvyne() {
      val vyne = vyneFactory.createVyne()
      expect(vyne).not.`null`
      vyne.schema.types.map { it.taxiType.qualifiedName }.should.contain.all.elements(
         "lang.taxi.Boolean",
         "lang.taxi.String",
         "lang.taxi.Int",
         "lang.taxi.Decimal",
         "lang.taxi.Date",
         "lang.taxi.Time",
         "lang.taxi.DateTime",
         "lang.taxi.Instant",
         "lang.taxi.Array",
         "lang.taxi.Any",
         "lang.taxi.Double",
         "lang.taxi.Void",
         "io.vyne.spring.MyClient"
      )
   }

   @VyneSchemaPublisher(publicationMethod = SchemaPublicationMethod.DISTRIBUTED)
   @EnableAutoConfiguration
   class vyneConfigWithDistributedSchemaStore {}
}


@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(classes = [PropertyConfig::class, LocalVyneClassPathSchemaFileConfigTest.vyneConfigWithLocalClassPathSchemaFile::class])
@TestPropertySource(properties = ["vyne.schema.name=testSchema", "vyne.schema.version=0.1.0", "spring.application.name=vyneTest"])
class LocalVyneClassPathSchemaFileConfigTest {

   @Autowired
   lateinit var vyneFactory: VyneFactory

   @MockBean
   internal var schemaStoreClient: SchemaStoreClient? = null

   @Test
   fun parsesContextCorrectly() {
      val vyne = vyneFactory!!.`object`
      expect(vyne).not.`null`
      expect(vyne.schema.services).to.have.size(1)

      vyne.schema.types.map { it.taxiType.qualifiedName }.should.contain.all.elements(
         "lang.taxi.Boolean",
         "lang.taxi.String",
         "lang.taxi.Int",
         "lang.taxi.Decimal",
         "lang.taxi.Date",
         "lang.taxi.Time",
         "lang.taxi.DateTime",
         "lang.taxi.Instant",
         "lang.taxi.Array",
         "lang.taxi.Any",
         "lang.taxi.Double",
         "lang.taxi.Void",
         "vyne.example.Client",
         "vyne.example.ClientId",
         "vyne.example.ClientName",
         "vyne.example.IsicCode"
      )
   }


   @VyneSchemaPublisher(publicationMethod = SchemaPublicationMethod.DISABLED, schemaFile = "foo.taxi")
   @EnableAutoConfiguration
   class vyneConfigWithLocalClassPathSchemaFile {}
}


@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(classes = [PropertyConfig::class, LocalVyneAnnotationControllerContextPathTest.vyneConfig::class])
@TestPropertySource(properties = ["vyne.schema.name=testSchema", "vyne.schema.version=0.1.0", "spring.application.name=vyneTest", "server.servlet.context-path=/test-microservice"])
class LocalVyneAnnotationControllerContextPathTest {

   @Autowired
   lateinit var vyneFactory: VyneFactory

   @MockBean
   internal var schemaStoreClient: SchemaStoreClient? = null

   @Test
   fun parsesContextCorrectly() {
      val vyne = vyneFactory!!.`object`
      expect(vyne).not.`null`
      expect(vyne.schema.services).to.have.size(1)

      val operations = vyne.schema.service("io.vyne.spring.MyService").operations
      expect(operations).to.have.size(1)
      expect(operations.first().metadata.first().params["url"]).equal("/test-microservice/getMyMethod")

   }

   @VyneSchemaPublisher(publicationMethod = SchemaPublicationMethod.DISABLED)
   @EnableAutoConfiguration()
   class vyneConfig {}

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
internal class MyService {

   @Operation
   @GetMapping("/getMyMethod")
   fun myMethod() = listOf<String>()
}


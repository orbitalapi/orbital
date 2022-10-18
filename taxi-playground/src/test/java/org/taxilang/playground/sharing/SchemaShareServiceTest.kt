package org.taxilang.playground.sharing

import com.winterbe.expekt.should
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.taxilang.playground.JpaConfig
import kotlin.test.assertFailsWith

@RunWith(SpringRunner::class)
@DataJpaTest(excludeAutoConfiguration = [FlywayAutoConfiguration::class], )
class SchemaShareServiceTest {

   @Autowired
   lateinit var repository: StoredSchemaRepository


   lateinit var service: SchemaShareService

   @Before
   fun setup() {
      service = SchemaShareService(repository)
   }

   @Test
   fun `can save a schema`() {
      val response = service.getShareableLink("hello world")
      val taxi = service.getStoredSchema(response.id).body!!
      taxi.should.equal("hello world")
   }

   @Test
   fun `cannot save an empty schema`() {
      assertFailsWith<BadRequestException>() {
         val response = service.getShareableLink("")
      }
   }

   @Test
   fun `cannot save a schema that is too large`() {
      val bigRandomString = RandomStringUtils.random(StoredSchema.MAX_SIZE + 10)
      assertFailsWith<BadRequestException> {
         val response = service.getShareableLink(bigRandomString)
      }
   }

   @Test
   fun `sharing the same content generates the same slug`() {
      val response1 = service.getShareableLink("hello world")
      service.getShareableLink("hello world").id.should.equal(response1.id)

      // Adding whitespace around the taxi should still generate the same response code
      service.getShareableLink("  hello world").id.should.equal(response1.id)
      service.getShareableLink("  hello world  ").id.should.equal(response1.id)
      service.getShareableLink("hello world  ").id.should.equal(response1.id)
   }
}

@Import(JpaConfig::class, SchemaShareService::class)
class TestConfig


package io.vyne.spring

import com.winterbe.expekt.should
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [ProjectPathSchemaSourceProviderSpringBootTest::class],
   properties = [
      "server.taxonomy-path=classpath:taxonomy"
   ])
class ProjectPathSchemaSourceProviderSpringBootTest {
   @Autowired
   lateinit var environment: ConfigurableEnvironment

   @Test
   fun `when projectPath refers to a property in application yml`() {
      val projectPathSimpleTaxiSchemaProvider =  ProjectPathSchemaSourceProvider("server.taxonomy-path", environment)
      projectPathSimpleTaxiSchemaProvider.schemaStrings().size.should.equal(1)
      val schema = projectPathSimpleTaxiSchemaProvider.schema()
      schema.hasType("CsvRow").should.be.`true`
   }
}

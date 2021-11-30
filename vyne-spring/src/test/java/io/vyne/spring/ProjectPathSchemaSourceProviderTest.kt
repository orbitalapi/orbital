package io.vyne.spring

import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import org.junit.Test
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

class ProjectPathSchemaSourceProviderTest {
   val environment = mock<ConfigurableEnvironment>()
   @Test
   fun `load from a folder in classpath`() {
      //see taxonomy folder in the classpath
      val projectPathSimpleTaxiSchemaProvider =  ProjectPathSchemaSourceProvider("classpath:taxonomy", environment)
      projectPathSimpleTaxiSchemaProvider.schemaStrings().size.should.equal(1)
      val schema = projectPathSimpleTaxiSchemaProvider.schema()
      schema.hasType("CsvRow").should.be.`true`
   }

   @Test
   fun `load from a taxi file in classpath`() {
      //see taxonomy folder in the classpath
      val projectPathSimpleTaxiSchemaProvider =  ProjectPathSchemaSourceProvider("classpath:foo.taxi", environment)
      projectPathSimpleTaxiSchemaProvider.schemaStrings().size.should.equal(1)
      val schema = projectPathSimpleTaxiSchemaProvider.schema()
      schema.hasType("Client").should.be.`true`
   }

   @Test
   fun `load from a folder in file system`() {
      //see taxonomy folder in the classpath
      val absolutePath = PathMatchingResourcePatternResolver().getResource("taxonomy").file.absolutePath
      val projectPathSimpleTaxiSchemaProvider =  ProjectPathSchemaSourceProvider("file:$absolutePath", environment)
      projectPathSimpleTaxiSchemaProvider.schemaStrings().size.should.equal(1)
      val schema = projectPathSimpleTaxiSchemaProvider.schema()
      schema.hasType("CsvRow").should.be.`true`
   }
}

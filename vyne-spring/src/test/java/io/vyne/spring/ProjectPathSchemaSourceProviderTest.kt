package io.vyne.spring

import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.schemaPublisherApi.loaders.FileSystemSourcesLoader
import io.vyne.schemaSpring.LoadableSchemaProject
import io.vyne.schemaSpring.ProjectPathSchemaSourceProvider
import org.junit.Test
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

class ProjectPathSchemaSourceProviderTest {
   val environment = mock<ConfigurableEnvironment>()

   @Test
   fun `load from a taxi file`() {
      val projectPathSimpleTaxiSchemaProvider =
         ProjectPathSchemaSourceProvider(
            LoadableSchemaProject("foo.taxi", FileSystemSourcesLoader::class.java),
            environment
         )
      projectPathSimpleTaxiSchemaProvider.schemaStrings().size.should.equal(1)
      val schema = projectPathSimpleTaxiSchemaProvider.schema()
      schema.hasType("Client").should.be.`true`
   }

   @Test
   fun `load from a folder in file system`() {
      val projectPathSimpleTaxiSchemaProvider =
         ProjectPathSchemaSourceProvider(
            LoadableSchemaProject("taxonomy", FileSystemSourcesLoader::class.java),
            environment
         )
      projectPathSimpleTaxiSchemaProvider.schemaStrings().size.should.equal(1)
      val schema = projectPathSimpleTaxiSchemaProvider.schema()
      schema.hasType("CsvRow").should.be.`true`
   }
}

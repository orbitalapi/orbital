package io.vyne.spring

import com.google.common.io.Resources
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.schemaPublisherApi.loaders.FileSystemSourcesLoader
import io.vyne.schemaSpring.LoadableSchemaProject
import io.vyne.schemaSpring.ProjectPathSchemaSourceProvider
import org.junit.Test
import org.springframework.core.env.ConfigurableEnvironment
import java.nio.file.Paths

class ProjectPathSchemaSourceProviderSpringBootTest {
   @Test
   fun `when projectPath refers to a property`() {
      val resourceUri = Resources.getResource("taxonomy").toURI()
      val environment = mock<ConfigurableEnvironment>()
      whenever(environment.getProperty("server.taxonomy-path")).thenReturn("taxonomy")
      val projectPathSimpleTaxiSchemaProvider = ProjectPathSchemaSourceProvider(
         listOf(
            LoadableSchemaProject(
               "server.taxonomy-path", FileSystemSourcesLoader::class.java
            )
         ), environment
      )
      projectPathSimpleTaxiSchemaProvider.schemaStrings().size.should.equal(1)
      val schema = projectPathSimpleTaxiSchemaProvider.schema()
      schema.hasType("CsvRow").should.be.`true`
   }
}

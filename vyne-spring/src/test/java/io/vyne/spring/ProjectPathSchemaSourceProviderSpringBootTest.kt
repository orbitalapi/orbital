package io.vyne.spring

import com.google.common.io.Resources
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.schema.publisher.loaders.FileSystemSourcesLoader
import io.vyne.schema.spring.LoadableSchemaProject
import io.vyne.schema.spring.ProjectPathSchemaSourceProvider
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test
import org.springframework.core.env.ConfigurableEnvironment
import kotlin.test.fail

class ProjectPathSchemaSourceProviderSpringBootTest {
   @Test
   fun `when projectPath refers to a property`() {
      fail("Fix this")
//      val resourceUri = Resources.getResource("taxonomy").toURI()
//      val environment = mock<ConfigurableEnvironment>()
//      whenever(environment.getProperty("server.taxonomy-path")).thenReturn("taxonomy")
//      val projectPathSourceProvider = ProjectPathSchemaSourceProvider(
//         listOf(
//            LoadableSchemaProject(
//               "server.taxonomy-path", FileSystemSourcesLoader::class.java
//            )
//         ), environment
//      )
//      projectPathSourceProvider.versionedSources.size.should.equal(1)
//      val schema = TaxiSchema.from(projectPathSourceProvider.versionedSources)
//      schema.hasType("CsvRow").should.be.`true`
   }
}

package io.vyne.spring

import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.schema.publisher.loaders.FileSystemSourcesLoader
import io.vyne.schema.spring.LoadableSchemaProject
import io.vyne.schema.spring.ProjectPathSchemaSourceProvider
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import java.nio.file.Paths

class ProjectPathSchemaSourceProviderTest {
   val environment = mock<ConfigurableEnvironment>()

   @Test
   fun `load from a taxi file`() {
      val projectPathSimpleTaxiSchemaProvider = ProjectPathSchemaSourceProvider(
         LoadableSchemaProject(FileSystemSourcesLoader(Paths.get("foo.taxi"))), environment
      )
      projectPathSimpleTaxiSchemaProvider.versionedSources.size.should.equal(1)
      val schema = TaxiSchema.from(projectPathSimpleTaxiSchemaProvider.versionedSources)
      schema.hasType("Client").should.be.`true`
   }

   @Test
   fun `load from a folder in file system`() {
      val projectPathSimpleTaxiSchemaProvider = ProjectPathSchemaSourceProvider(
         LoadableSchemaProject(FileSystemSourcesLoader(Paths.get("taxonomy"))), environment
      )
      projectPathSimpleTaxiSchemaProvider.versionedSources.size.should.equal(1)
      val schema = TaxiSchema.from(projectPathSimpleTaxiSchemaProvider.versionedSources)
      schema.hasType("CsvRow").should.be.`true`
   }
}

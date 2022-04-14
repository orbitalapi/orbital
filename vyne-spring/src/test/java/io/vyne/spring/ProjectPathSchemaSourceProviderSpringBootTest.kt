package io.vyne.spring

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.schemaPublisherApi.loaders.FileSystemSourcesLoader
import io.vyne.schemaSpring.LoadableSchemaProject
import io.vyne.schemaSpring.ProjectPathSchemaSourceProvider
import org.junit.Test
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.ClassPathResource

class ProjectPathSchemaSourceProviderSpringBootTest {
   @Test
   fun `when projectPath refers to a property`() {
      val environment = mock<ConfigurableEnvironment>()
      val taxonomyPath = ClassPathResource("foo.taxi").file.absolutePath
      whenever(environment.resolvePlaceholders("\${vyne.services.schema}")).thenReturn(taxonomyPath)
      val projectPathSimpleTaxiSchemaProvider = ProjectPathSchemaSourceProvider(
         listOf(
            LoadableSchemaProject(
               "\${vyne.services.schema}", FileSystemSourcesLoader::class.java
            )
         ), environment
      )
      projectPathSimpleTaxiSchemaProvider.schemaStrings().size.should.equal(1)
      val schema = projectPathSimpleTaxiSchemaProvider.schema()
      schema.hasType("vyne.example.Client").should.be.`true`
   }
}

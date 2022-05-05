package io.vyne.spring

import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.schema.spring.ProjectPathSchemaSourceProvider
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class ProjectPathSchemaSourceProviderTest {

   @Test
   fun `load from a taxi file`() {
      val projectPathSimpleTaxiSchemaProvider = ProjectPathSchemaSourceProvider(
         Resources.getResource("foo.taxi")
      )
      projectPathSimpleTaxiSchemaProvider.versionedSources.size.should.equal(1)
      val schema = TaxiSchema.from(projectPathSimpleTaxiSchemaProvider.versionedSources)
      schema.hasType("Client").should.be.`true`
   }

   @Test
   fun `load from a folder in file system`() {
      val projectPathSimpleTaxiSchemaProvider = ProjectPathSchemaSourceProvider(
         Resources.getResource("taxonomy")
      )
      projectPathSimpleTaxiSchemaProvider.versionedSources.size.should.equal(1)
      val schema = TaxiSchema.from(projectPathSimpleTaxiSchemaProvider.versionedSources)
      schema.hasType("CsvRow").should.be.`true`
   }
}

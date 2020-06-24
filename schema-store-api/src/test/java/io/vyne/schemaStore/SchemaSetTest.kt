package io.vyne.schemaStore

import com.winterbe.expekt.should
import io.vyne.VersionedSource
import org.junit.Assert.*
import org.junit.Test

class SchemaSetTest {
//
//   @Test
//   fun when_addingNewSchema_then_generationIsIncremented() {
//      var schemaSet = SchemaSet.EMPTY
//      schemaSet = schemaSet.offerSource(VersionedSource("foo", "0.1.2", ""))
//      schemaSet.generation.should.equal(0)
//      schemaSet = schemaSet.add(VersionedSource("bar", "0.1.2", ""))
//      schemaSet.generation.should.equal(1)
//      schemaSet.size().should.equal(2)
//   }
//
//   @Test
//   fun when_addingNewSchemaWithHigherVersion_then_existingVersionIsRemoved() {
//      var schemaSet = SchemaSet.EMPTY.add(VersionedSource("foo", "0.1.0", ""))
//      schemaSet.size().should.equal(1)
//
//      schemaSet = schemaSet.add(VersionedSource("foo", "0.1.2", ""))
//      schemaSet.size().should.equal(1)
//      val schema = schemaSet.sources.first()
//      schema.name.should.equal("foo")
//      schema.source.version.should.equal("0.1.2")
//   }
//
//   @Test
//   fun when_addingNewSchemaWithLowerVersion_then_existingVersionIsRetained() {
//      val originalSchemaSet = SchemaSet.EMPTY.add(VersionedSource("foo", "0.1.0", ""))
//      originalSchemaSet.size().should.equal(1)
//
//      val schemaSet = originalSchemaSet.add(VersionedSource("foo", "0.0.8", ""))
//      originalSchemaSet.should.equal(schemaSet)
//   }

   @Test
   fun when_constructingSchemaSetWithMultipleVersions_then_onlyLatestIsTaken() {
      val schemaSet = SchemaSet.from(
         listOf(
            VersionedSource("foo", "0.1.0", ""),
            VersionedSource("foo", "0.1.1", ""),
            VersionedSource("bar", "0.1.1", "")
         ), 1
      )

      schemaSet.size().should.equal(2)
      schemaSet.contains("foo", "0.1.1").should.be.`true`
      schemaSet.contains("foo", "0.1.0").should.be.`false`
      schemaSet.contains("bar", "0.1.1").should.be.`true`
   }

}

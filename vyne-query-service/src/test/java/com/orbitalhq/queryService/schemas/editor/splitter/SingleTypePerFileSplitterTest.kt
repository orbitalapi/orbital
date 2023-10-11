package com.orbitalhq.queryService.schemas.editor.splitter

import com.winterbe.expekt.should
import com.orbitalhq.cockpit.core.schemas.editor.splitter.SingleTypePerFileSplitter
import com.orbitalhq.schemas.taxi.TaxiSchema
import lang.taxi.types.CompilationUnit
import lang.taxi.types.ImportableToken
import org.junit.Test

class SingleTypePerFileSplitterTest {
   @Test
   fun splitsSimpleSchemaToMultipleSourceFiles() {
      val taxi = TaxiSchema.from(
         """
         namespace com.orbitalhq.test
         type FirstName inherits String
         model Person {
            firstName : FirstName
         }
      """
      ).taxi
      val sources = SingleTypePerFileSplitter.toVersionedSources(taxi.types.getCompilationUnits())
      sources[0].name.should.equal("com/orbitalhq/test/FirstName.taxi")
      sources[0].content.should.equal(
         """namespace com.orbitalhq.test {
   type FirstName inherits String
}"""
      )
      sources[1].name.should.equal("com/orbitalhq/test/Person.taxi")
      sources[1].content.should.equal(
         """import com.orbitalhq.test.FirstName
namespace com.orbitalhq.test {
   model Person {
               firstName : FirstName
            }
}"""
      )
   }

   @Test
   fun `type that makes reference to placeholder type splits correctly`() {
      val taxi = TaxiSchema.from(
         """
         namespace com.orbitalhq.test

         type FirstName // Does not inherit from anything
      """
      ).taxi
      val sources = SingleTypePerFileSplitter.toVersionedSources(taxi.types.getCompilationUnits())
      sources[0].name.should.equal("com/orbitalhq/test/FirstName.taxi")
      sources[0].content.should.equal("""namespace com.orbitalhq.test {
   type FirstName
}""")
   }

   @Test
   fun `service declarations include imports`() {
      val taxi = TaxiSchema.from(
         """
         namespace com.orbitalhq.test
         type FirstName inherits String
         model Person {
            firstName : FirstName
         }
         service PersonService {
            operation findPeopleWithName(FirstName):Person[]
         }
      """
      ).taxi
      val sources = SingleTypePerFileSplitter.toVersionedSources(taxi.services.getCompilationUnits())
      sources[0].name.should.equal("com/orbitalhq/test/PersonService.taxi")
      sources[0].content.should.equal("""import com.orbitalhq.test.FirstName
import com.orbitalhq.test.Person
namespace com.orbitalhq.test {
   service PersonService {
               operation findPeopleWithName(FirstName):Person[]
            }
}""")
   }


   private fun Iterable<ImportableToken>.getCompilationUnits(): List<Pair<ImportableToken, List<CompilationUnit>>> =
      this.map { it to it.compilationUnits }
}

package io.vyne.queryService.schemas.editor.splitter

import com.winterbe.expekt.should
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.types.CompilationUnit
import lang.taxi.types.ImportableToken
import org.junit.Test

class SingleTypePerFileSplitterTest {
   @Test
   fun splitsSimpleSchemaToMultipleSourceFiles() {
      val taxi = TaxiSchema.from(
         """
         namespace io.vyne.test
         type FirstName inherits String
         model Person {
            firstName : FirstName
         }
      """
      ).taxi
      val sources = SingleTypePerFileSplitter.toVersionedSources(taxi.types.getCompilationUnits())
      sources[0].name.should.equal("io/vyne/test/FirstName.taxi")
      sources[0].content.should.equal(
         """namespace io.vyne.test {
   type FirstName inherits String
}"""
      )
      sources[1].name.should.equal("io/vyne/test/Person.taxi")
      sources[1].content.should.equal(
         """import io.vyne.test.FirstName
namespace io.vyne.test {
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
         namespace io.vyne.test

         type FirstName // Does not inherit from anything
      """
      ).taxi
      val sources = SingleTypePerFileSplitter.toVersionedSources(taxi.types.getCompilationUnits())
      sources[0].name.should.equal("io/vyne/test/FirstName.taxi")
      sources[0].content.should.equal("""namespace io.vyne.test {
   type FirstName
}""")
   }

   @Test
   fun `service declarations include imports`() {
      val taxi = TaxiSchema.from(
         """
         namespace io.vyne.test
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
      sources[0].name.should.equal("io/vyne/test/PersonService.taxi")
      sources[0].content.should.equal("""import io.vyne.test.FirstName
import io.vyne.test.Person
namespace io.vyne.test {
   service PersonService {
               operation findPeopleWithName(FirstName):Person[]
            }
}""")
   }


   private fun Iterable<ImportableToken>.getCompilationUnits(): List<Pair<ImportableToken, List<CompilationUnit>>> =
      this.map { it to it.compilationUnits }
}

package io.vyne.schemas.taxi

import com.winterbe.expekt.should
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test

class TaxiSchemaTest {


   @Test
   fun `calling from() with compiler errors returns empty schema`() {
      // This behaviour is important, otherwise the query server can fail to start
      val schema = TaxiSchema.from("i am invalid")
      schema.should.not.be.`null`
   }

   @Test
   fun `parses a schema with a message stream correctly`() {
      val service = TaxiSchema.from(
         """
         model Person
         service MyKafkaService {
            stream personEvents : Stream<Person>
         }
      """.trimIndent()
      ).service("MyKafkaService")
      service.streamOperations.should.have.size(1)
      val stream = service.streamOperations.single()
      stream.returnType.name.parameterizedName.should.equal("lang.taxi.Stream<Person>")
   }

   @Test
   fun `parses a schema with a table correctly`() {
      val service = TaxiSchema.from(
         """
         model Person
         service MyKafkaService {
            table person : Person[]
         }
      """.trimIndent()
      ).service("MyKafkaService")
      service.tableOperations.should.have.size(1)
      val table = service.tableOperations.single()
      table.returnType.name.parameterizedName.should.equal("lang.taxi.Array<Person>")
   }


   @Test
   fun `formatted types are defined on fields`() {
      val schema = TaxiSchema.from("""
            @Format("dd/MM/yy'T'HH:mm:ss" )
            type MyDate inherits Instant

            model Person {
               fromType : MyDate
               @Format("yyyy-MM-dd HH:mm:ss")
               fromTypeWithFormat : MyDate

               @Format(offset = 60)
               fromTypeWithOffset : MyDate
            }
      """.trimIndent())
      schema.type("MyDate").format!!.shouldContainExactly("dd/MM/yy'T'HH:mm:ss" )
      val person = schema.type("Person")
      person.attribute("fromType").format!!.patterns.shouldContainExactly("dd/MM/yy'T'HH:mm:ss" )
      person.attribute("fromTypeWithFormat").format!!.patterns.shouldContainExactly("yyyy-MM-dd HH:mm:ss")
      person.attribute("fromTypeWithOffset").format!!.patterns.shouldContainExactly("dd/MM/yy'T'HH:mm:ss" )
      person.attribute("fromTypeWithOffset").format!!.utcZoneOffsetInMinutes!!.shouldBe(60)
   }
}

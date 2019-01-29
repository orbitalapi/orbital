package io.vyne.query.policyManager

import com.winterbe.expekt.expect
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.policies.FilterInstruction
import org.junit.Before
import org.junit.Test

class FilterInstructionExecutorTest {

   lateinit var person: TypedObject
   @Before
   fun setup() {
      val taxi = """
type Person {
   firstName : String
   lastName : String
   age : Int
} """.trimIndent()
      val schema = TaxiSchema.from(taxi)
      person = TypedObject.fromAttributes("Person", mapOf("firstName" to "Jimmy", "lastName" to "Spitts", "age" to 25), schema)
   }

   @Test
   fun given_instructionFiltersAll_then_nullIsReturned() {
      val filtered = FilterInstructionExecutor().execute(FilterInstruction(), person)
      expect(filtered).to.be.instanceof(TypedNull::class.java)
   }

   @Test
   fun given_instructionFiltersAttribute_then_filteredRecordIsReturned() {
      val filtered = FilterInstructionExecutor().execute(FilterInstruction(fieldNames = listOf("lastName", "age")), person)
      expect(filtered).to.be.instanceof(TypedObject::class.java)
      val filteredObject = filtered as TypedObject
      expect(filteredObject["firstName"].value).to.equal("Jimmy")
      expect(filteredObject["lastName"].value).to.be.`null`
      expect(filteredObject["age"].value).to.be.`null`
   }
}

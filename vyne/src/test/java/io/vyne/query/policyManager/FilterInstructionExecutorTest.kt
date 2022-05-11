package io.vyne.query.policyManager

import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.ConversionService
import io.vyne.models.DefinedInSchema
import io.vyne.models.Provided
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.models.TypedValue
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.policies.FilterInstruction
import lang.taxi.policies.LiteralArraySubject
import org.junit.Before
import org.junit.Test

class FilterInstructionExecutorTest {

   lateinit var person: TypedObject
   @Before
   fun setup() {
      val taxi = """
type FirstName inherits String
type Age inherits Int

type Person {
   firstName : FirstName
   lastName : String
   age : Age
}

policy PersonPolicy against Person {
   read {
      case caller.FirstName == "Joe" -> filter (lastName)
      else -> permit
   }
}""".trimIndent()
      val schema = TaxiSchema.from(taxi)
      person = TypedObject.fromAttributes("Person", mapOf("firstName" to "Jimmy", "lastName" to "Spitts", "age" to 25), schema, source = Provided)
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

   @Test
   fun EqualOperatorEvaluatorTest() {
      val evaluator: OperatorEvaluator = EqualOperatorEvaluator()
      val firstName = TypedValue.from(person["firstName"].type, "Joe", ConversionService.DEFAULT_CONVERTER, DefinedInSchema)
      val age = TypedValue.from(person["age"].type, 25, ConversionService.DEFAULT_CONVERTER, DefinedInSchema)

      evaluator.evaluate(age, 25).should.be.`true`
      evaluator.evaluate(age, 33).should.be.`false`
      evaluator.evaluate(age, "25").should.be.`false`
      evaluator.evaluate(firstName, "Joe").should.be.`true`
      evaluator.evaluate(firstName, "Jimi").should.be.`false`
      evaluator.evaluate("1", 1).should.be.`false`
      evaluator.evaluate(1, 1.0).should.be.`false`
      evaluator.evaluate(1.0, 1).should.be.`false`
      evaluator.evaluate(1.2, 1.2).should.be.`true`
      evaluator.evaluate(1.2, 2.3).should.be.`false`
   }

   @Test
   fun NotEqualOperatorEvaluatorTest() {
      val evaluator: OperatorEvaluator = NotEqualOperatorEvaluator(EqualOperatorEvaluator())
      val firstName = TypedValue.from(person["firstName"].type, "Joe", ConversionService.DEFAULT_CONVERTER, DefinedInSchema)
      val age = TypedValue.from(person["age"].type, 25, ConversionService.DEFAULT_CONVERTER, DefinedInSchema)

      evaluator.evaluate(age, 25).should.be.`false`
      evaluator.evaluate(age, 33).should.be.`true`
      evaluator.evaluate(age, "25").should.be.`true`
      evaluator.evaluate(firstName, "Joe").should.be.`false`
      evaluator.evaluate(firstName, "Jimi").should.be.`true`
      evaluator.evaluate("1", 1).should.be.`true`
      evaluator.evaluate(1, 1.0).should.be.`true`
      evaluator.evaluate(1.0, 1).should.be.`true`
      evaluator.evaluate(1.2, 1.2).should.be.`false`
      evaluator.evaluate(1.2, 2.3).should.be.`true`
   }

   @Test
   fun InOperatorEvaluatorTest() {
      val evaluator: OperatorEvaluator = InOperatorEvaluator()
      val firstName = TypedValue.from(person["firstName"].type, "Joe", ConversionService.DEFAULT_CONVERTER, DefinedInSchema)
      val age = TypedValue.from(person["age"].type, 25, ConversionService.DEFAULT_CONVERTER, DefinedInSchema)

      evaluator.evaluate(age, LiteralArraySubject(listOf(1, 25, 3))).should.be.`true`
      evaluator.evaluate(age, LiteralArraySubject(listOf(0, 2, 3))).should.be.`false`
      evaluator.evaluate(firstName, LiteralArraySubject(listOf("Joe", "Jimi", "Herb"))).should.be.`true`
      evaluator.evaluate(firstName, LiteralArraySubject(listOf("Pat", "Jimi", "Herb"))).should.be.`false`
   }
}

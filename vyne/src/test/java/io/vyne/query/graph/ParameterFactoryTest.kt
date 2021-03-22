package io.vyne.query.graph

import com.winterbe.expekt.should
import io.vyne.Vyne
import io.vyne.models.TypedNull
import io.vyne.models.json.addKeyValuePair
import io.vyne.query.graph.operationInvocation.UnresolvedOperationParametersException
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/*
class ParameterFactoryTest {

   lateinit var vyne: Vyne

   @Before
   fun setup() {
      val (vyne, _) = io.vyne.testVyne("""type Person {
      |firstName : FirstName as String
      |lastName : LastName as String
      |}
   """.trimMargin())
      this.vyne = vyne
   }

   @Test
   fun findsParameterInContext() {
      vyne.addKeyValuePair("FirstName", "Jimmy")
      val result = runBlocking {ParameterFactory().discover(vyne.type("FirstName"), vyne.query().queryEngine.queryContext())}
      result.value.should.equal("Jimmy")
   }

   @Test(expected = UnresolvedOperationParametersException::class)
   fun typedNullsAreNotReturned() {
      vyne.addModel(TypedNull.create(vyne.type("FirstName")))
      runBlocking {ParameterFactory().discover(vyne.type("FirstName"), vyne.query().queryEngine.queryContext())}
   }

   @Test(expected = UnresolvedOperationParametersException::class)
   // This is a contentious call... see the notes in the method
   // re implementation.
   fun emptyStringsAreNotReturned() {
      vyne.addKeyValuePair("FirstName", "")
      runBlocking {ParameterFactory().discover(vyne.type("FirstName"), vyne.query().queryEngine.queryContext())}
   }
}
*/

package com.orbitalhq.functions.scanner

import com.orbitalhq.functions.ReturnType
import com.orbitalhq.functions.TaxiFunction
import com.orbitalhq.functions.TaxiFunctionProvider
import com.orbitalhq.functions.TaxiParam
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import lang.taxi.functions.FunctionAccessor

class GreetingFunctions : TaxiFunctionProvider {
   @TaxiFunction(description = "Greets a user by name")
   fun sayHello(@TaxiParam(name = "name") name: String): String {
      return "Hello, $name"
   }

   @TaxiFunction(name = "addNumbers")
   fun add(
      @TaxiParam(name = "x") x: Int,
      @TaxiParam(name = "y") y: Int
   ): Int = x + y
}


class TestFunctions : TaxiFunctionProvider {
   @TaxiFunction
   fun greet(@TaxiParam name: String): String = "Hello, $name"

   @TaxiFunction(name = "addNumbers")
   fun add(@TaxiParam x: Int, @TaxiParam y: Int): Int = x + y

   @TaxiFunction
   fun needsSchema(schema: Schema): String = "schema=$schema"

   @TaxiFunction
   fun needsReturnType(@ReturnType type: Type): String = "type=$type"

   @TaxiFunction
   fun needsFunctionAccessor(fa: FunctionAccessor): String = "fa=${fa::class.simpleName}"
}

class TaxiFunctionScannerTest : DescribeSpec({
   describe("TaxiFunctionScanner") {
      val scanner = TaxiFunctionScanner()
      val provider = GreetingFunctions()

      describe("Function argument bindings") {
         val functions = scanner.scan(TestFunctions())
         it("should bind @TaxiParam as ArgBinding") {
            functions.forName("greet")
               .bindings.single()
               .shouldBeInstanceOf<ArgBinding>()
         }

         it("should bind multiple @TaxiParam values") {
            val bindings = functions.forName("addNumbers").bindings
            bindings.shouldHaveSize(2)
            bindings[0].shouldBeInstanceOf<ArgBinding>()
            bindings[1].shouldBeInstanceOf<ArgBinding>()
         }

         it("should bind Schema as SchemaBinding") {
            functions.forName("needsSchema")
               .bindings.single()
               .shouldBeInstanceOf<SchemaBinding>()
         }

         it("should bind ReturnType as ReturnTypeBinding") {
            functions.forName("needsReturnType")
               .bindings.single()
               .shouldBeInstanceOf<ReturnTypeBinding>()
         }

         it("should bind FunctionAccessor as FunctionAccessorBinding") {
            functions.forName("needsFunctionAccessor")
               .bindings.single()
               .shouldBeInstanceOf<FunctionAccessorBinding>()
         }
      }
   }
})


fun List<BoundFunction>.forName(name: String): BoundFunction {
   return this.firstOrNull { it.name == name }
      ?: error("No found named $name found")
}

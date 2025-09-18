package com.orbitalhq.functions.taxigen

import com.orbitalhq.functions.TaxiFunction
import com.orbitalhq.functions.TaxiFunctionProvider
import com.orbitalhq.functions.TaxiParam
import com.orbitalhq.functions.scanner.TaxiFunctionScanner
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.mpp.newInstanceNoArgConstructor
import io.vavr.control.Either
import lang.taxi.Compiler
import kotlin.reflect.KClass
import kotlin.test.fail

class TaxiFunctionDefinitionGeneratorTest : DescribeSpec({

   describe("TaxiFunctionDefinitionGenerator") {

      it("should generate parameter names from @TaxiParam") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction(description = "Greets a user by name")
            fun greet(@TaxiParam(name = "name") name: String): String = "Hello, $name"
         }

         val taxi = generateFor(TestFunctions::class)

         taxi.shouldCompile()
         taxi shouldContain "greet(name: lang.taxi.String)"
      }

      it("should prefer explicit @TaxiParam.type over JVM type") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction
            fun overrideType(@TaxiParam(name = "person", type = "com.foo.PersonName") person: String): String = "ok"
         }

         val taxi = generateFor(TestFunctions::class)

//         taxi.shouldCompile()
         taxi shouldContain "overrideType(person: com.foo.PersonName): lang.taxi.String"
      }

      it("should mark Kotlin nullable parameters as optional with ?") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction
            fun nullableParam(@TaxiParam name: String?): String = "ok"
         }

         val taxi = generateFor(TestFunctions::class)


         taxi.shouldCompile()
         taxi shouldContain "nullableParam(name: lang.taxi.String?)"
      }

      it("should generate correct return type for primitives") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction
            fun add(@TaxiParam(name = "x") x: Int, @TaxiParam(name = "y") y: Int): Int = x + y
         }

         val taxi = generateFor(TestFunctions::class)

         taxi.shouldCompile()
         taxi shouldContain "add(x: lang.taxi.Int, y: lang.taxi.Int): lang.taxi.Int"
      }

      it("should use an explicit return type if provided") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction(returnType = "com.foo.SummedValue")
            fun add(@TaxiParam(name = "x") x: Int, @TaxiParam(name = "y") y: Int): Int = x + y
         }

         val taxi = generateFor(TestFunctions::class)

         // Don't compile - summedValue isn't declared
//         taxi.shouldCompile()
         taxi shouldContain "add(x: lang.taxi.Int, y: lang.taxi.Int): com.foo.SummedValue"
      }

      it("should generate return type unwrapping Kotlin Result<>") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction
            fun greet(@TaxiParam name: String): Result<String> = Result.success("Hello")
         }

         val taxi = generateFor(TestFunctions::class)

         taxi.shouldCompile()
         taxi shouldContain "greet(name: lang.taxi.String): lang.taxi.String"
      }

      it("should generate return type unwrapping Either<>") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction
            fun greet(@TaxiParam name: String): io.vavr.control.Either<String, String> =Either.right("Hello")
         }

         val taxi = generateFor(TestFunctions::class)

         taxi.shouldCompile()
         taxi shouldContain "greet(name: lang.taxi.String): lang.taxi.String"
      }

      it("should include markdown description in [[ ]]") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction(description = "Greets a user by name")
            fun greet(@TaxiParam(name = "name") name: String): String = "Hello, $name"
         }

         val taxi = generateFor(TestFunctions::class)

         taxi.shouldCompile()
         taxi shouldContain "[["
         taxi shouldContain "Greets a user by name"
         taxi shouldContain "]]"
      }
   }
})

private fun generateFor(klass: KClass<out TaxiFunctionProvider>):String {
   val functions = TaxiFunctionScanner().scan(klass.newInstanceNoArgConstructor())
   val taxi = TaxiFunctionDefinitionGenerator.generate(functions)
   return taxi
}
private fun String.shouldCompile() {
   val errors = Compiler.forStrings(this).validate()
   if (errors.isNotEmpty()) {
      fail(
         "Expected source to compile, but found ${errors.size} compilation errors: \n" +
            errors.joinToString("\n") { it.detailMessage }
      )
   }
}

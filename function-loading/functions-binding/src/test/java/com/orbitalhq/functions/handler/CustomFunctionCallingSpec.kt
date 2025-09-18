package com.orbitalhq.functions.handler

import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.firstRawObject
import com.orbitalhq.firstTypedObject
import com.orbitalhq.functions.ReturnType
import com.orbitalhq.functions.TaxiFunction
import com.orbitalhq.functions.TaxiFunctionProvider
import com.orbitalhq.functions.TaxiParam
import com.orbitalhq.functions.loaders.CustomFunctionSourceGenerator
import com.orbitalhq.models.FailedEvaluatedExpression
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.UndefinedSource
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.vavr.control.Either
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.PrimitiveType

class BoundFunctionSpec : DescribeSpec({

   describe("BoundFunction return scenarios") {

      it("handles plain values") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction
            fun greet(@TaxiParam name: String): String = "Hello, $name"
         }

         val (vyne) = testVyne(TaxiSchema.from(listOf(functionsAsSourcePackage(TestFunctions()))))
         vyne.query("""find { greeting:String = greet("Jimmy") }""")
            .firstRawObject() shouldBe mapOf("greeting" to "Hello, Jimmy")
      }

      it("handles TypedInstance return") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction(returnType = "lang.taxi.String")
            fun wrap(@TaxiParam name: String, schema: Schema): TypedInstance =
               TypedInstance.from(schema.type(PrimitiveType.STRING), "Wrapped: $name", schema, source = UndefinedSource)
         }

         val (vyne) = testVyne(TaxiSchema.from(listOf(functionsAsSourcePackage(TestFunctions()))))
         vyne.query("""find { greeting:String = wrap("Jimmy") }""")
            .firstRawObject() shouldBe mapOf("greeting" to "Wrapped: Jimmy")
      }

      it("returns TypedNull when the returned TypedInstance is of the wrong type") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction(returnType = "lang.taxi.String")
            fun wrap(@TaxiParam name: String, schema: Schema): TypedInstance =
               TypedInstance.from(schema.type(PrimitiveType.INTEGER), 20, schema, source = UndefinedSource)
         }

         val (vyne) = testVyne(TaxiSchema.from(listOf(functionsAsSourcePackage(TestFunctions()))))
         val result = vyne.query("""find { greeting:String = wrap("Jimmy") }""")
            .firstTypedObject()
         val typedNull = result["greeting"].shouldBeInstanceOf<TypedNull>()
         typedNull.source.shouldBeInstanceOf<FailedEvaluatedExpression>()
            .errorMessage.shouldBe("Custom function com.orbitalhq.functions.handler.wrap returned a TypedInstance of type lang.taxi.Int which is not assignable to the declared type lang.taxi.String - returning null")
      }
      it("handles Kotlin Result.success") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction
            fun greet(@TaxiParam name: String): Result<String> =
               Result.success("Hello, $name")
         }

         val (vyne) = testVyne(TaxiSchema.from(listOf(functionsAsSourcePackage(TestFunctions()))))
         vyne.query("""find { greeting:String = greet("Jimmy") }""")
            .firstRawObject() shouldBe mapOf("greeting" to "Hello, Jimmy")
      }

      it("handles Kotlin Result.failure") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction
            fun fail(): Result<String> =
               Result.failure(RuntimeException("Boom"))
         }

         val (vyne) = testVyne(TaxiSchema.from(listOf(functionsAsSourcePackage(TestFunctions()))))
         val result = vyne.query("""find { greeting:String = fail() }""")
            .firstTypedObject()

         val typedNull = result["greeting"].shouldBeInstanceOf<TypedNull>()
         typedNull.source.shouldBeInstanceOf<FailedEvaluatedExpression>()
            .errorMessage.shouldBe("Invoking function com.orbitalhq.functions.handler.fail failed with exception RuntimeException - Boom")
      }

      it("handles Either.right") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction
            fun greet(@TaxiParam name: String): Either<String, String> =
               Either.right("Hello, $name")
         }

         val (vyne) = testVyne(TaxiSchema.from(listOf(functionsAsSourcePackage(TestFunctions()))))
         vyne.query("""find { greeting:String = greet("Jimmy") }""")
            .firstRawObject() shouldBe mapOf("greeting" to "Hello, Jimmy")
      }

      it("handles Either.left Throwable") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction
            fun fail(): Either<Throwable, String> =
               Either.left(RuntimeException("Fail"))
         }

         val (vyne) = testVyne(TaxiSchema.from(listOf(functionsAsSourcePackage(TestFunctions()))))
         val result = vyne.query("""find { greeting:String = fail() }""")
            .firstTypedObject()
         val typedNull = result["greeting"].shouldBeInstanceOf<TypedNull>()
         typedNull.source.shouldBeInstanceOf<FailedEvaluatedExpression>()
            .errorMessage.shouldBe("Invoking function com.orbitalhq.functions.handler.fail failed with exception RuntimeException - Fail")

      }

      it("handles Either.left non-Throwable") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction
            fun fail(): Either<String, String> =
               Either.left("Bad input")
         }

         val (vyne) = testVyne(TaxiSchema.from(listOf(functionsAsSourcePackage(TestFunctions()))))
         val result = vyne.query("""find { greeting:String = fail() }""")
            .firstTypedObject()
         val typedNull = result["greeting"].shouldBeInstanceOf<TypedNull>()
         typedNull.source.shouldBeInstanceOf<FailedEvaluatedExpression>()
            .errorMessage.shouldBe("Bad input")
      }

      it("handles null return") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction
            fun nothing(@TaxiParam name: String): String? = null
         }

         val (vyne) = testVyne(TaxiSchema.from(listOf(functionsAsSourcePackage(TestFunctions()))))
         val result = vyne.query("""find { greeting:String = nothing("Jimmy") }""")
            .firstTypedObject()
         result["greeting"]
            .shouldBeInstanceOf<TypedNull>()
      }
   }

   describe("BoundFunction input binding scenarios") {

      it("passes primitive args") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction
            fun add(@TaxiParam x: Int, @TaxiParam y: Int): Int = x + y
         }

         val (vyne) = testVyne(TaxiSchema.from(listOf(functionsAsSourcePackage(TestFunctions()))))
         vyne.query("""find { sum:Int = add(2,3) }""")
            .firstRawObject() shouldBe mapOf("sum" to 5)
      }

      it("injects Schema") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction
            fun needsSchema(schema: Schema): String = "Schema: ${schema.types.size}"
         }

         val (vyne) = testVyne(TaxiSchema.from(listOf(functionsAsSourcePackage(TestFunctions()))))
         val result = vyne.query("""find { val:String = needsSchema() }""").firstRawObject()
         result["val"].toString().startsWith("Schema:") shouldBe true
      }

      it("injects ReturnType") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction
            fun needsReturnType(@ReturnType type: Type): String =
               "Type: ${type.paramaterizedName}"
         }

         val (vyne) = testVyne(TaxiSchema.from(listOf(functionsAsSourcePackage(TestFunctions()))))
         val result = vyne.query("""find { val:String = needsReturnType() }""").firstRawObject()
         result["val"].toString().startsWith("Type:") shouldBe true
      }

      it("injects FunctionAccessor") {
         class TestFunctions : TaxiFunctionProvider {
            @TaxiFunction
            fun needsAccessor(accessor: FunctionAccessor): String =
               "Accessor: ${accessor.function.qualifiedName}"
         }

         val (vyne) = testVyne(TaxiSchema.from(listOf(functionsAsSourcePackage(TestFunctions()))))
         val result = vyne.query("""find { val:String = needsAccessor() }""").firstRawObject()
         result["val"].toString().startsWith("Accessor:") shouldBe true
      }
   }
})


fun functionsAsSourcePackage(testFunctions: TaxiFunctionProvider): SourcePackage {
   return CustomFunctionSourceGenerator.sourceGeneratorForFunctionProviderInstances(listOf(testFunctions))
      .generateSourcePackage(emptyList(), PackageMetadata.from("com.foo", "test"))
}

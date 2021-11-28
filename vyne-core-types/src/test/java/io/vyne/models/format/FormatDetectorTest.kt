package io.vyne.models.format

import com.winterbe.expekt.should
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class FormatDetectorTest {

   private val formatDetector = FormatDetector(listOf(FixModelSpec,AvroModelSpec))

   @Test
   fun `detects format on annotated`() {
      val schema = TaxiSchema.from(
         """
         @FixModel
         model MyModel {
         }
      """.trimIndent()
      )
      formatDetector.getFormatType(schema.type("MyModel"))?.second.should.equal(FixModelSpec)
   }

   @Test
   fun `detects format on array of annotated type`() {
      val schema = TaxiSchema.from(
         """
         @FixModel
         model MyModel {
         }
      """.trimIndent()
      )
      formatDetector.getFormatType(schema.type("MyModel[]"))?.second.should.equal(FixModelSpec)
   }

   @Test
   fun `detects format on array of subtype of annotated type`() {
      val schema = TaxiSchema.from(
         """
         @FixModel
         model MyModel {
         }
         model MySuperModel inherits MyModel
      """.trimIndent()
      )
      formatDetector.getFormatType(schema.type("MySuperModel"))?.second.should.equal(FixModelSpec)
   }
   @Test
   fun `when subtype is annotated then subtype annotation overrides base type annotation in a collection`() {
      val schema = TaxiSchema.from(
         """
         @FixModel
         model MyModel {
         }
         @AvroModel
         model MySuperModel inherits MyModel
      """.trimIndent()
      )
      formatDetector.getFormatType(schema.type("MySuperModel[]"))?.second.should.equal(AvroModelSpec)
   }

   @Test
   fun `when subtype is annotated then subtype annotation overrides base type annotation`() {
      val schema = TaxiSchema.from(
         """
         @FixModel
         model MyModel {
         }
         @AvroModel
         model MySuperModel inherits MyModel
      """.trimIndent()
      )
      formatDetector.getFormatType(schema.type("MySuperModel"))?.second.should.equal(AvroModelSpec)
   }
   @Test
   fun `when supertype is annotated then superType annotation is detected many levels deep`() {
      val schema = TaxiSchema.from(
         """
         @FixModel
         model MyModel {
         }
         model MyModel1 inherits MyModel
         model MyModel2 inherits MyModel1
         model MyModel3 inherits MyModel2
      """.trimIndent()
      )
      formatDetector.getFormatType(schema.type("MyModel3"))?.second.should.equal(FixModelSpec)
   }
   @Test
   fun `when subtype is annotated then superType annotation is detected many levels deep`() {
      val schema = TaxiSchema.from(
         """
         @FixModel
         model MyModel {
         }
         model MyModel1 inherits MyModel
         @AvroModel
         model MyModel2 inherits MyModel1
         model MyModel3 inherits MyModel2
      """.trimIndent()
      )
      formatDetector.getFormatType(schema.type("MyModel3"))?.second.should.equal(AvroModelSpec)
   }


   @Test
   fun `returns null when no parser present`() {
      val schema = TaxiSchema.from(
         """
         @NotTheCorrectAnnotationType
         model MyModel {
         }
      """.trimIndent()
      )
      formatDetector.getFormatType(schema.type("MyModel[]")).should.be.`null`
   }
   @Test
   fun `returns null when no parser annotation type present`() {
      val schema = TaxiSchema.from(
         """
         model MyModel {
         }
      """.trimIndent()
      )
      formatDetector.getFormatType(schema.type("MyModel[]")).should.be.`null`
   }
}

abstract class StubSpec : ModelFormatSpec {
   override val deserializer: ModelFormatDeserializer
      get() = TODO("Not yet implemented")
   override val serializer: ModelFormatSerializer
      get() = TODO("Not yet implemented")
}
object FixModelSpec : StubSpec() {
   override val annotations: List<QualifiedName> = listOf("FixModel".fqn())
}
object AvroModelSpec : StubSpec() {
   override val annotations: List<QualifiedName> = listOf("AvroModel".fqn())
}

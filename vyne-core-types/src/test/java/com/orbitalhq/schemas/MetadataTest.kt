package com.orbitalhq.schemas

import com.orbitalhq.from
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.winterbe.expekt.should
import io.kotest.matchers.shouldBe
import org.junit.Assert.*
import org.junit.Test

class MetadataTest {
   @Test
   fun `metadata with single param generates taxi correctly`() {
      Metadata(
         "com.test.Approved".fqn(),
         params = mapOf("approverName" to "Jimmy")
      ).asTaxi()
         .should.equal("""@com.test.Approved(approverName = "Jimmy")""")
   }

   @Test
   fun `metadata with multiple param generates taxi correctly`() {
      Metadata(
         "com.test.Approved".fqn(),
         params = mapOf(
            "approverName" to "Jimmy",
            "approvedWith" to "Thanks"
         )
      ).asTaxi()
         .should.equal("""@com.test.Approved(approverName = "Jimmy", approvedWith = "Thanks")""")
   }


   @Test
   fun `metadata with qualified name generates taxi correctly`() {
      Metadata("com.test.Approved".fqn()).asTaxi()
         .should.equal("@com.test.Approved")
   }

   @Test
   fun `metadata without qualified name generates taxi correctly`() {
      Metadata("Approved".fqn()).asTaxi()
         .should.equal("@Approved")
   }

   @Test
   fun `passing an enum by enum reference is returned as string`() {
      val schema = TaxiSchema.from("""
         enum Switch {
            On, Off
         }
         annotation Toggle {
            enabled : Switch
         }
         @Toggle(enabled = Switch.On)
         model Person {
         }
      """.trimIndent())
      schema.type("Person")
         .getMetadata("Toggle".fqn())
         .params["enabled"]
         .shouldBe("On")
   }

   @Test
   fun `passing an enum by enum name is returned as string`() {
      val schema = TaxiSchema.from("""
         enum Switch {
            On, Off
         }
         annotation Toggle {
            enabled : Switch
         }
         @Toggle(enabled = 'On')
         model Person {
         }
      """.trimIndent())
      schema.type("Person")
         .getMetadata("Toggle".fqn())
         .params["enabled"]
         .shouldBe("On")
   }

   @Test
   fun `passing an enum by default value is returned as string`() {
      val schema = TaxiSchema.from("""
         enum Switch {
            On, Off
         }
         annotation Toggle {
            enabled : Switch = Switch.On
         }
         @Toggle
         model Person {
         }
      """.trimIndent())
      schema.type("Person")
         .getMetadata("Toggle".fqn())
         .params["enabled"]
         .shouldBe("On")
   }
}

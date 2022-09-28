package io.vyne.schemas

import com.winterbe.expekt.should
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
}

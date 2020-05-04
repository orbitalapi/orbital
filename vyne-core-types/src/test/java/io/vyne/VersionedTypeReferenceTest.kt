package io.vyne

import com.winterbe.expekt.should
import io.vyne.schemas.fqn
import lang.taxi.packages.PackageIdentifier
import org.junit.Test

class VersionedTypeReferenceTest {

   @Test
   fun canParse() {
      val typeRef = VersionedTypeReference(
         "foo.Bar<baz.Fun>".fqn(),
         PackageIdentifier.fromId("com.acme/TestRepo/0.1.0")
      )

      val stringRef = typeRef.toString()
      val parsed = VersionedTypeReference.parse(stringRef)
      parsed.should.equal(typeRef)
   }

   @Test
   fun unversionedDoesntIncludeVersionData() {
      val typeRef = VersionedTypeReference(
         "foo.Bar<baz.Fun>".fqn(),
         PackageIdentifier.UNSPECIFIED
      )

      val stringRef = typeRef.toString()
      stringRef.should.equal("foo.Bar<baz.Fun>")

      val parsed = VersionedTypeReference.parse(stringRef)
      parsed.should.equal(typeRef)
   }
}

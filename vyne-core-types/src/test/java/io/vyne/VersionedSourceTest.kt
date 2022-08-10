package io.vyne

import com.github.zafarkhaja.semver.Version
import com.winterbe.expekt.should
import io.vyne.VersionedSource
import org.junit.Test

class VersionedSourceTest {

   @Test
   fun parsesSemverCorrectly() {
      val schema = VersionedSource("foo", "0.2.3", "")
      schema.semver.should.equal(Version.valueOf("0.2.3"))
   }

   @Test
   fun returnsDefaultVersionIfSemverIsInvalid() {
      val schema = VersionedSource("foo", "badVersion", "")
      schema.semver.majorVersion.should.equal(0)
      schema.semver.minorVersion.should.equal(0)
      schema.semver.patchVersion.should.equal(0)
      schema.semver.buildMetadata.should.not.be.`null`

   }

   @Test
   fun `can prepend and remove a package identifier`() {
      val source = VersionedSource("foo.taxi", "0.2.3", "")
      val packageIdentifier = PackageIdentifier(
         "com.acme",
         "taxonomy",
         "1.0.0"
      )
      val prepended = source.prependPackageIdentifier(
         packageIdentifier
      )
      prepended.name.should.equal("[com.acme/taxonomy/1.0.0]/foo.taxi")
      val (removedId,cleanedSource) = prepended.removePackageIdentifier()
      removedId.should.equal(packageIdentifier)
      cleanedSource.should.equal(source)
   }

   @Test
   fun `prepending package identifier multiple times has no effect`() {
      val source = VersionedSource("foo.taxi", "0.2.3", "")
      val packageIdentifier = PackageIdentifier(
         "com.acme",
         "taxonomy",
         "1.0.0"
      )
      val prepended = source.prependPackageIdentifier(
         packageIdentifier
      ).prependPackageIdentifier(packageIdentifier).prependPackageIdentifier(packageIdentifier)

      prepended.name.should.equal("[com.acme/taxonomy/1.0.0]/foo.taxi")
   }

   @Test
   fun `if no package identifier then splitting returns null`() {
      val source = VersionedSource("foo.taxi", "0.2.3", "")
      val (removedId,cleanedSource) = source.removePackageIdentifier()
      removedId.should.be.`null`
      cleanedSource.should.equal(source)
   }
}


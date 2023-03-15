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


}


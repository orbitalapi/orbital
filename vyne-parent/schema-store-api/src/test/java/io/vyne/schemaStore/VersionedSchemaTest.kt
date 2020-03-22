package io.vyne.schemaStore

import com.github.zafarkhaja.semver.Version
import com.winterbe.expekt.should
import org.junit.Test

class VersionedSchemaTest {

   @Test
   fun parsesSemverCorrectly() {
      val schema = VersionedSchema("foo", "0.2.3", "")
      schema.semver.should.equal(Version.valueOf("0.2.3"))
   }

   @Test
   fun returnsDefaultVersionIfSemverIsInvalid() {
      val schema = VersionedSchema("foo", "badVersion", "")
      schema.semver.majorVersion.should.equal(0)
      schema.semver.minorVersion.should.equal(0)
      schema.semver.patchVersion.should.equal(0)
      schema.semver.buildMetadata.should.not.be.`null`

   }
}

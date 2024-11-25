package com.orbitalhq.schemaServer.core.repositories.upload

import com.google.common.io.Resources
import com.orbitalhq.PackageIdentifier
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TaxiProjectUploadHandlerTest {

   // Root of the zip is a directory - ie., user clicked on the parent directory, and chose to zip
   @Test
   fun `can read package identifier from taxi conf in zip file when root entry is a folder`() {
      val byteArray = zipByteArray("sample-zip-with-parent-folder.zip")
      val packageIdentifier = TaxiProjectUploadHandler().readPackageIdentifier(byteArray)
      packageIdentifier.shouldBe(PackageIdentifier.fromId("org.taxi/sample/0.3.0"))
   }

   // Root of zip is the taxi project -- ie., user clicked on the files themselves
   // and chose "add to zip"
   @Test
   fun `can read package identifier from taxi conf in zip file when root folder contains files`() {
      val byteArray = zipByteArray("sample-zip-without-parent-folder.zip")
      val packageIdentifier = TaxiProjectUploadHandler().readPackageIdentifier(byteArray)
      packageIdentifier.shouldBe(PackageIdentifier.fromId("org.taxi/sample/0.3.0"))
   }


   @Test
   fun `throws error when zip is malformed`() {
      val malformedByteArray = "I am not a zip file!".toByteArray()
      assertThrows<InvalidZipException> { TaxiProjectUploadHandler().readPackageIdentifier(malformedByteArray)  }
   }

   @Test
   fun `throws error when no taxi present in zip file`() {
      val byteArray = zipByteArray("zip-without-taxi-conf.zip")
      assertThrows<MissingConfigurationException> { TaxiProjectUploadHandler().readPackageIdentifier(byteArray)  }
   }

   @Test
   fun `throws error when taxi file is malformed`() {
      val byteArray = zipByteArray("zip-with-bad-taxi-conf-file.zip")
      assertThrows<InvalidConfigurationException> { TaxiProjectUploadHandler().readPackageIdentifier(byteArray)  }
   }

   private fun zipByteArray(zipFileName: String):ByteArray {
      return Resources.toByteArray(Resources.getResource("zipped-projects/$zipFileName"))
   }
}

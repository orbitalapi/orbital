package com.orbitalhq.schemaServer.core.file.packages

import com.google.common.io.Resources
import com.winterbe.expekt.should
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.packages.OpenApiPackageLoaderSpec
import org.junit.Test
import reactor.kotlin.test.test
import java.nio.file.Paths

class FileSystemOpenApiPackageLoaderTest : BaseFileSystemPackageLoaderTest() {

   @Test
   fun `can load an openApi package from disk`() {
      val specPath = Paths.get(
         Resources.getResource("open-api/petstore-expanded.yaml")
            .toURI()
      )

      val packageSpec = FileSystemPackageSpec(
         specPath,
         OpenApiPackageLoaderSpec(
            identifier = PackageIdentifier(
               "com.acme",
               "petstore-api",
               "0.2.0"
            ),
            defaultNamespace = "com.acme"
         )
      )

      val (fileMonitor, loader) = buildLoader(packageSpec)
      loader.start()
         .test()
         .expectSubscription()
         .expectNextMatches { schemaPackage ->
            schemaPackage.sourcesWithPackageIdentifier.should.have.size(1)
            schemaPackage.identifier.should.equal(
               PackageIdentifier(
                  organisation = "com.acme",
                  name = "petstore-api",
                  version = "0.2.0" // The version from the package spec overrides what's in the OAS spec
               )
            )
            true
         }
         .thenCancel()
         .verify()
   }
}

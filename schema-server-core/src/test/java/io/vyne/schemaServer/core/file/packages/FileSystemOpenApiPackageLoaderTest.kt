package io.vyne.schemaServer.core.file.packages

import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.PackageIdentifier
import io.vyne.schemaServer.core.adaptors.OpenApiPackageLoaderSpec
import io.vyne.schemaServer.core.file.FileSystemPackageSpec
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
            schemaPackage.sources.should.have.size(1)
            schemaPackage.identifier.should.equal(
               PackageIdentifier(
                  organisation = "com.acme",
                  name = "petstore-api",
                  version = "1.0.0" // Version is taken from the OpenApi spec, not what's passed in
               )
            )
            true
         }
         .thenCancel()
         .verify()
   }
}

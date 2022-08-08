package io.vyne.schemaServer.core.file.packages

import com.winterbe.expekt.should
import io.vyne.schemaServer.core.adaptors.TaxiPackageLoaderSpec
import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.core.file.deployProject
import org.junit.Test
import reactor.kotlin.test.test
import java.nio.file.Files

class FileSystemTaxiPackageLoaderTest : BaseFileSystemPackageLoaderTest() {


   @Test
   fun `can load a taxi package from disk`() {
      projectHome.deployProject("sample-project")
      val packageSpec = FileSystemPackageSpec(
         projectHome.root.toPath(),
         TaxiPackageLoaderSpec
      )

      val (fileMonitor, loader) = buildLoader(packageSpec)

      loader.start()
         .test()
         .expectSubscription()
         .expectNextMatches { schemaPackage ->
            schemaPackage.sources.should.be.empty
            true
         }
         .then {
            val createdFile = Files.createFile(projectHome.root.toPath().resolve("src/hello.taxi"))
            createdFile.toFile().writeText("type Name inherits String")
            fileMonitor.pollNow()
         }
         .expectNextMatches { schemaPackage ->
            schemaPackage.sources.should.have.size(1)
            true
         }
         .thenCancel()
         .verify()
   }

   @Test
   fun `can load an openapi package from disk`() {

   }

}

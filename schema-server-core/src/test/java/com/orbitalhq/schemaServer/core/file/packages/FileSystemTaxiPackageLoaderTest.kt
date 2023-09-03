package com.orbitalhq.schemaServer.core.file.packages

import com.winterbe.expekt.should
import com.orbitalhq.schemaServer.packages.TaxiPackageLoaderSpec
import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.file.deployProject
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
            schemaPackage.sourcesWithPackageIdentifier.should.be.empty
            true
         }
         .then {
            val createdFile = Files.createFile(projectHome.root.toPath().resolve("src/hello.taxi"))
            createdFile.toFile().writeText("type Name inherits String")
            fileMonitor.pollNow()
         }
         .expectNextMatches { schemaPackage ->
            schemaPackage.sourcesWithPackageIdentifier.should.have.size(1)
            true
         }
         .thenCancel()
         .verify()
   }

   @Test
   fun `can load an openapi package from disk`() {

   }

}
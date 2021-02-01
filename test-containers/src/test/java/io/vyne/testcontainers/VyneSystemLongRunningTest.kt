package io.vyne.testcontainers

import com.winterbe.expekt.should
import io.vyne.testcontainers.CommonSettings.latestSnapshot
import org.junit.jupiter.api.Test
import java.io.File

class VyneSystemLongRunningTest {
   private val statusUp = """{"status":"UP"}"""

   @Test
   fun `Eureka Based Schema Distribution With File Schema Server reading the taxonomy from a git repo`() {
      VyneSystem.withEurekaAndGitBasedSchema(
         "bank-schema",
         "master",
         "git@gitlab.com:notional/SomeProject/some-taxonomy.git",
         "/home/Joe/.ssh/id_rsa_new",
         latestSnapshot
      ).use {
         // Note that verifyPublishedSchemaByFileSchemaServer set to false whilst creating EurekaBasedSystemVerifier instance
         // as above 'git' details are fictitious
         it.start(EurekaBasedSystemVerifier(verifyPublishedSchemaByFileSchemaServer = false))
         it.fileSchemaServer.getActuatorHealthStatus().should.equal(statusUp)
         it.vyneQueryServer.getActuatorHealthStatus().should.equal(statusUp)
         it.caskServer.getActuatorHealthStatus().should.equal(statusUp)
      }

   }

   @Test
   fun `Eureka Based Schema Distribution With File Schema Server reading the taxonomy from a local folder`() {
      val taxonomyPath = File("src/test/resources/taxonomy")
      val caskDataCsv = File("src/test/resources/vyne.test.containers.Product.csv")
      VyneSystem.withEurekaAndFileBasedSchema(taxonomyPath.absolutePath)
         .use {
            it.start()
            it.vyneQueryServer.postCsvDataFile(caskDataCsv).should.equal(200)
            it.vyneQueryServer.ensureService("vyne.casks.vyne.test.containers.ProductCaskService")
            val queryResult = it.vyneQueryServer.submitVyneQl("findAll { vyne.test.containers.Product[]  }")
            queryResult.should.not.be.empty
         }
   }
}

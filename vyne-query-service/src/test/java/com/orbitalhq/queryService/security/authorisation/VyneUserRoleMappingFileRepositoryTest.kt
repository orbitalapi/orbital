package com.orbitalhq.queryService.security.authorisation

import com.google.common.io.Resources
import com.winterbe.expekt.should
import com.orbitalhq.auth.authorisation.VyneConsumerType
import com.orbitalhq.auth.authorisation.VyneUserRoleMappingFileRepository
import com.orbitalhq.auth.authorisation.VyneUserRoles
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.URI

class VyneUserRoleMappingFileRepositoryTest {
   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Test
   fun `can read value from disk`() {
      val configFile = configFileInTempFolder("authorisation/user-role-mappings.conf", folder)
      val repository = VyneUserRoleMappingFileRepository(configFile.toPath())
      val rolesForUser1 = repository.findByUserName("user1")
      rolesForUser1?.roles.should.equal(
         setOf(
            "Viewer",
            "QueryRunner",
            "PlatformManager")
      )
      rolesForUser1?.type.should.equal(VyneConsumerType.USER.name)
      VyneConsumerType.API.name.should.equal(repository.findByUserName("queryExecutor")?.type)
   }

   @Test
   fun `can update user role`() {
      val userName = "userWhoseRolesTobeModified"
      val configFile = configFileInTempFolder("authorisation/user-role-mappings.conf", folder)
      val repository = VyneUserRoleMappingFileRepository(configFile.toPath())
      repository.save(userName, VyneUserRoles(setOf("Admin")))
      repository.findByUserName(userName)!!.roles.should.equal(setOf("Admin"))

   }

   @Test
   fun `can delete a user role`() {
      val configFile = configFileInTempFolder("authorisation/user-role-mappings.conf", folder)
      val repository = VyneUserRoleMappingFileRepository(configFile.toPath())
      repository.save(
         "user2", VyneUserRoles(roles = setOf("Viewer"))
      )
      repository.deleteByUserName("user2")

      val writtenSource = configFile.readText()
      repository.findByUserName("user2").should.be.`null`
      repository.findByUserName("user1").should.not.be.`null`
   }

   companion object {
      fun configFileInTempFolder(resourceName: String, temporaryFolder: TemporaryFolder): File {
         return Resources.getResource(resourceName).toURI()
            .copyTo(temporaryFolder.root)
      }
   }
}

fun URI.copyTo(destDirectory: File): File {
   val source = File(this)
   val destFile = destDirectory.resolve(source.name)
   FileUtils.copyFile(source, destFile)
   return destFile
}
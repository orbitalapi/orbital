package com.orbitalhq.queryService.security

import com.google.common.io.Resources
import com.orbitalhq.auth.authentication.ConfigFileVyneUserRepository
import com.orbitalhq.auth.authentication.VyneUser
import com.orbitalhq.auth.authorisation.VyneUserRoleDefinitionFileRepository
import com.orbitalhq.auth.authorisation.VyneUserRoleDefinitionRepository
import com.orbitalhq.auth.authorisation.VyneUserRoleMappingFileRepository
import com.orbitalhq.auth.authorisation.VyneUserRoleMappingRepository
import com.orbitalhq.cockpit.core.security.UserService
import com.orbitalhq.cockpit.core.security.authorisation.VyneOpenIdpConnectConfig
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.core.io.ClassPathResource
import reactor.test.StepVerifier
import java.io.File
import java.net.URI

class UserServiceTest {
   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Test
   fun `fetch all Vyne users from config file`() {
      val vyneUserRoleMappingRepository: VyneUserRoleMappingRepository =
         VyneUserRoleMappingFileRepository(configFileInTempFolder("authorisation/user-role-mappings.conf").toPath())
      val vyneUserRoleDefinitionRepository: VyneUserRoleDefinitionRepository =
         VyneUserRoleDefinitionFileRepository(ClassPathResource("authorisation/vyne-authorisation-role-definitions.conf").file.toPath())
      val configFile = configFileInTempFolder("users/users.conf")
      val configFileVyneUserRepository = ConfigFileVyneUserRepository(configFile.toPath())
      val userService = UserService(
         configFileVyneUserRepository,
         vyneUserRoleMappingRepository,
         vyneUserRoleDefinitionRepository,
         VyneOpenIdpConnectConfig()
      )
      StepVerifier
         .create(userService.vyneUsers())
         .expectNext(VyneUser("userId1", "stuncay", "serhat.tuncay@vyne.co", "http://vyne/stuncay", "Serhat Tuncay"))
         .expectNext(VyneUser("userId2", "mpitt", "marty.pitt@vyne.co", "http://vyne/mpitt", "Marty Pitt"))
         .expectComplete()
         .verify()
   }

   private fun configFileInTempFolder(resourceName: String): File {
      return Resources.getResource(resourceName).toURI()
         .copyTo(folder.root)
   }

   private fun URI.copyTo(destDirectory: File): File {
      val source = File(this)
      val destFile = destDirectory.resolve(source.name)
      FileUtils.copyFile(source, destFile)
      return destFile
   }
}
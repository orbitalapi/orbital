package io.vyne.jwt.auth.keycloack

import io.vyne.jwt.auth.config.KeycloakServerProperties
import io.vyne.jwt.auth.config.RegularJsonConfigProviderFactory
import io.vyne.jwt.auth.config.log
import org.keycloak.Config
import org.keycloak.credential.CredentialAuthentication
import org.keycloak.credential.PasswordCredentialProvider
import org.keycloak.models.KeycloakSession
import org.keycloak.models.RealmModel
import org.keycloak.models.UserCredentialModel
import org.keycloak.representations.idm.RealmRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.keycloak.services.managers.ApplianceBootstrap
import org.keycloak.services.managers.RealmManager
import org.keycloak.services.resources.KeycloakApplication
import org.keycloak.storage.UserStorageManager
import org.keycloak.util.JsonSerialization
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource

class EmbeddedKeycloakApplication: KeycloakApplication() {
   init {
      createMasterRealmAdminUser()
      createVyneRealm()
   }

   override fun loadConfig() {
      val factory = RegularJsonConfigProviderFactory()
      Config.init(factory.create().orElseThrow { NoSuchElementException("No value present") })
   }

   private fun createMasterRealmAdminUser() {
      val session = getSessionFactory().create()
      val applianceBootstrap = ApplianceBootstrap(session)
      val admin = keycloakServerProperties.adminUser
      try {
         session.transactionManager
            .begin()
         applianceBootstrap.createMasterRealmUser(admin.username, admin.password)
         session.transactionManager
            .commit()
      } catch (ex: Exception) {
        log().warn("Couldn't create keycloak master admin user: ${ex.message}")
         session.transactionManager
            .rollback()
      }
      session.close()
   }

   private fun createVyneRealm() {
      val session = getSessionFactory().create()
      try {
         session.transactionManager
            .begin()
         val manager = RealmManager(session)

         val lessonRealmImportFile: Resource = ClassPathResource(keycloakServerProperties.realmImportFile)
         val realmRepresentation = JsonSerialization.readValue(lessonRealmImportFile.inputStream, RealmRepresentation::class.java)
         val realmModel = manager.importRealm(realmRepresentation)
         addUsers(realmModel, session)
         session.transactionManager
            .commit()
      } catch (ex: java.lang.Exception) {
         log().warn("Failed to import Realm json file: {} ${ex.message}")
         session.transactionManager
            .rollback()
      }
      session.close()
   }

   private fun addUsers(realmModel: RealmModel, session: KeycloakSession) {
      val existingUserNames = session.users().getUsers(realmModel).map { it.username }
      session.userCredentialManager()
      keycloakServerProperties
         .realmUsers
         .filter { realmUser -> !existingUserNames.contains(realmUser.username) }
         .map { realmUser ->
            val user = session.users().addUser(realmModel, realmUser.username)
            user.isEnabled = true
            session
               .userCredentialManager()
               .updateCredential(realmModel, user, UserCredentialModel.password(realmUser.password))
         }
   }

   companion object {
      lateinit var keycloakServerProperties: KeycloakServerProperties
   }
}

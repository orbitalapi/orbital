package io.vyne.jwt.auth.keycloack

import io.vyne.jwt.auth.config.KeycloakServerProperties
import io.vyne.jwt.auth.config.RegularJsonConfigProviderFactory
import io.vyne.jwt.auth.config.log
import mu.KotlinLogging
import org.apache.commons.io.IOUtils
import org.keycloak.Config
import org.keycloak.exportimport.ExportImportManager
import org.keycloak.models.KeycloakSession
import org.keycloak.models.RealmModel
import org.keycloak.models.UserCredentialModel
import org.keycloak.representations.idm.RealmRepresentation
import org.keycloak.services.managers.ApplianceBootstrap
import org.keycloak.services.managers.RealmManager
import org.keycloak.services.resources.KeycloakApplication
import org.keycloak.util.JsonSerialization
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import kotlin.io.path.exists

private val logger = KotlinLogging.logger {  }
class EmbeddedKeycloakApplication: KeycloakApplication() {

   override fun bootstrap(): ExportImportManager {
      val exportImportManager = super.bootstrap()
      createMasterRealmAdminUser()
      createVyneRealm()
      return exportImportManager
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

        val realmConfigFileStream = if (!keycloakServerProperties.realmImportFile.toFile().exists()) {
            logger.info { "No realm definition found at ${keycloakServerProperties.realmImportFile.toFile().canonicalPath}. using the default vyne-realm.json in classpath" }
            ClassPathResource("vyne-realm.json").inputStream
         } else {
           logger.info { "importing the realm definition found at ${keycloakServerProperties.realmImportFile.toFile().canonicalPath}" }
            keycloakServerProperties.realmImportFile.toFile().inputStream()
         }

         val realmRepresentation = JsonSerialization.readValue(realmConfigFileStream, RealmRepresentation::class.java)
         val realmModel = manager.importRealm(realmRepresentation)
         addStaticallyConfiguredUsers(realmModel, session)
         session.transactionManager
            .commit()
      } catch (ex: java.lang.Exception) {
         log().warn("Failed to import Realm json file: {} ${ex.message}")
         session.transactionManager
            .rollback()
      }
      session.close()
   }

   private fun addStaticallyConfiguredUsers(realmModel: RealmModel, session: KeycloakSession) {
      val existingUserNames = session.users().getUsers(realmModel).map { it.username }
      session.userCredentialManager()
      keycloakServerProperties
         .realmUsers
         .filter { realmUser -> !existingUserNames.contains(realmUser.username) }
         .map { realmUser ->
            val user = session.users().addUser(realmModel, realmUser.username)
            user.isEnabled = true
            user.email = realmUser.email
            session
               .userCredentialManager()
               .updateCredential(realmModel, user, UserCredentialModel.password(realmUser.password))
         }
   }

   companion object {
      lateinit var keycloakServerProperties: KeycloakServerProperties
   }
}

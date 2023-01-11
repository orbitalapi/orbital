package io.vyne.auth.authorisation

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.vyne.config.BaseHoconConfigFileRepository
import io.vyne.config.toConfig
import org.http4k.quoted
import java.nio.file.Path

class VyneUserRoleMappingFileRepository(
   path: Path,
   fallback: Config = ConfigFactory.systemEnvironment()
)
   : VyneUserRoleMappingRepository, BaseHoconConfigFileRepository<VyneUserRoleMappings>(path, fallback) {
   override fun extract(config: Config): VyneUserRoleMappings = config.extract()

   override fun emptyConfig(): VyneUserRoleMappings = VyneUserRoleMappings()

   override fun findByUserName(userName: String): VyneUserRoles? {
      val config = typedConfig()
      return config.userRoleMappings[userName]
   }

   override fun save(userName: String, roleMapping: VyneUserRoles): VyneUserRoles {
      val newConfig = ConfigFactory.empty()
         .withValue(userRoleMappingPath(userName), roleMapping.toConfig().root())
      val existingValues = unresolvedConfig()
      val updated = ConfigFactory.empty()
         .withFallback(newConfig)
         .withFallback(existingValues)
      saveConfig(updated)
      return typedConfig().userRoleMappings[userName]!!
   }

   override fun findAll(): Map<VyneUserName, VyneUserRoles> {
      return typedConfig().userRoleMappings.toMap()
   }

   override fun deleteByUserName(userName: String) {
      saveConfig(
         unresolvedConfig()
            .withoutPath(userRoleMappingPath(userName))
      )
   }

   override fun size(): Int {
      return typedConfig().userRoleMappings.size
   }

   private fun userRoleMappingPath(userName: String): String {
      return "userRoleMappings.${userName.quoted()}"
   }
}

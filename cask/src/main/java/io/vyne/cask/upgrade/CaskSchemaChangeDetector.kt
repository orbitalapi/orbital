package io.vyne.cask.upgrade

import io.vyne.cask.api.CaskConfig
import io.vyne.cask.api.CaskStatus
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.query.CaskConfigService
import io.vyne.cask.query.CaskDAO
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import io.vyne.schemas.fqn
import io.vyne.utils.log
import org.springframework.stereotype.Component

data class CaskNeedingUpgrade(val config: CaskConfig, val newType: VersionedType)

@Component
class CaskSchemaChangeDetector(private val caskConfigRepository: CaskConfigRepository,
                               private val caskConfigService: CaskConfigService,
                               private val caskDAO: CaskDAO
) {

   internal fun markModifiedCasksAsRequiringUpgrading(schema: Schema):List<CaskNeedingUpgrade> {
      val tablesToUpgrade = findCasksRequiringUpgrading(schema)
      return tablesToUpgrade.map { caskToUpgrade ->
         log().info("Cask for type ${caskToUpgrade.config.qualifiedTypeName} will be migrated from table ${caskToUpgrade.config.tableName} to ${caskToUpgrade.config.replacedByTableName}")
         val caskWithUpgradeConfig = markForMigration(caskToUpgrade)
         log().info("Ensuring table ${caskWithUpgradeConfig.config.replacedByTableName} exists")
         val generatedTableName = caskDAO.createCaskRecordTable(caskWithUpgradeConfig.newType)
         require(generatedTableName == caskWithUpgradeConfig.config.replacedByTableName) { "Generated table name for target cask does not match expected.  Expected ${caskToUpgrade.config.replacedByTableName} but got ${generatedTableName}"}
         caskWithUpgradeConfig
      }
   }

   private fun markForMigration(caskNeedingUpgrade: CaskNeedingUpgrade):CaskNeedingUpgrade {
      val newConfig = caskConfigService.createCaskConfig(caskNeedingUpgrade.newType)
      val migratingCask = caskNeedingUpgrade.config.copy(
         status = CaskStatus.MIGRATING,
         replacedByTableName = newConfig.tableName
      )
      log().info("Tagging that cask with table ${migratingCask.tableName} (which is type ${caskNeedingUpgrade.config.qualifiedTypeName} @${caskNeedingUpgrade.config.versionHash} is migrating to ${newConfig.tableName} (which is the type @${newConfig.versionHash})")
      caskConfigRepository.save(migratingCask)
      return caskNeedingUpgrade.copy(config = migratingCask)
   }

   internal fun findCasksRequiringUpgrading(schema: Schema): List<CaskNeedingUpgrade> {
      val casksRequiringUpgrading = caskConfigRepository.findAllByStatus(CaskStatus.ACTIVE)
         .mapNotNull { caskConfig ->
            val schemaVersionedType = schema.versionedType(caskConfig.qualifiedTypeName.fqn())
            val currentType = schemaVersionedType.taxiType
            val currentTypeContentHash = currentType.definitionHash
            when {
               currentTypeContentHash == null -> {
                  log().warn("Type ${currentType.qualifiedName} does not expose a content hash.  Cannot determine if this has changed.  Upgrades will not be performed against this type")
                  null
               }
               currentTypeContentHash != caskConfig.versionHash -> {
                  log().info("Type definition ${currentType.qualifiedName} has changed.  Was ${caskConfig.versionHash}, but is now ${currentTypeContentHash}.  This cask will be upgraded")
                  CaskNeedingUpgrade(caskConfig, schemaVersionedType)
               }
               else -> {
                  // Nothing to do
                  null
               }
            }
         }
      return casksRequiringUpgrading
   }
}

package io.vyne.cask.upgrade

import io.vyne.cask.api.CaskConfig
import io.vyne.cask.api.CaskStatus
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.ddl.views.CaskViewService
import io.vyne.cask.query.CaskConfigService
import io.vyne.cask.query.CaskDAO
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import io.vyne.schemas.fqn
import io.vyne.utils.log
import org.springframework.stereotype.Component

data class CaskNeedingUpgrade(val config: CaskConfig, val newType: VersionedType, val dependantViews: List<CaskConfig> = emptyList())

@Component
class CaskSchemaChangeDetector(private val caskConfigRepository: CaskConfigRepository,
                               private val caskConfigService: CaskConfigService,
                               private val caskDAO: CaskDAO,
                               private val caskViewService: CaskViewService
) {

   internal fun markModifiedCasksAsRequiringUpgrading(schema: Schema): List<CaskNeedingUpgrade> {
      val tablesToUpgrade = findCasksRequiringUpgrading(schema)
      return tablesToUpgrade.mapNotNull { caskToUpgrade ->
         log().info("Cask for type ${caskToUpgrade.config.qualifiedTypeName} will be migrated from table ${caskToUpgrade.config.tableName} to ${caskToUpgrade.config.replacedByTableName}")
         val caskWithUpgradeConfig = markForMigration(caskToUpgrade)
         if (caskWithUpgradeConfig != null) {
            log().info("Ensuring table ${caskWithUpgradeConfig.config.replacedByTableName} exists")
            val generatedTableName = caskDAO.createCaskRecordTable(caskWithUpgradeConfig.newType)
            require(generatedTableName == caskWithUpgradeConfig.config.replacedByTableName) { "Generated table name for target cask does not match expected.  Expected ${caskToUpgrade.config.replacedByTableName} but got ${generatedTableName}" }
         }
         caskWithUpgradeConfig
      }
   }

   private fun markForMigration(caskNeedingUpgrade: CaskNeedingUpgrade): CaskNeedingUpgrade? {
      val newConfig = caskConfigService.createCaskConfig(caskNeedingUpgrade.newType)
      val migratingCask = caskNeedingUpgrade.config.copy(
         status = CaskStatus.MIGRATING,
         replacedByTableName = newConfig.tableName
      )
      log().info("Tagging that cask with table ${migratingCask.tableName} (which is type ${caskNeedingUpgrade.config.qualifiedTypeName} @${caskNeedingUpgrade.config.versionHash} is migrating to ${newConfig.tableName} (which is the type @${newConfig.versionHash})")
      caskConfigRepository.save(migratingCask)
      log().info("Deleting cask views: ${caskNeedingUpgrade.dependantViews.joinToString { it.tableName }}")
      val allViewsDropped = caskNeedingUpgrade.dependantViews.all {
         caskViewService.deleteView(it)
      }
      if (!allViewsDropped) {
         log().error("Failed to drop all dependent views.  Cannot upgrade this cask")
         return null
      }
      return caskNeedingUpgrade.copy(config = migratingCask)
   }

   internal fun findCasksRequiringUpgrading(schema: Schema): List<CaskNeedingUpgrade> {
      val casksRequiringUpgrading = caskConfigRepository
         .findAllByStatusAndExposesType(CaskStatus.ACTIVE, false) // exclude views.
         .mapNotNull { caskConfig ->
            try {
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
                     val dependantViews = caskViewService.getViewDependenciesForType(caskConfig)
                     if (dependantViews.isNotEmpty()) {
                        log().info("As part of the upgrade of type ${currentType.qualifiedName}, the following views will be temporarily removed and recreated: ${dependantViews.joinToString { it.qualifiedTypeName }}")
                     }
                     CaskNeedingUpgrade(caskConfig, schemaVersionedType, dependantViews)
                  }
                  else -> {
                     // Nothing to do
                     null
                  }
               }
            } catch (e: Exception) {
               log().error("Error in processing $caskConfig")
               null
            }
         }
      return casksRequiringUpgrading
   }
}

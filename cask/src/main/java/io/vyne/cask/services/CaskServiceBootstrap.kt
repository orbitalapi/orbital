package io.vyne.cask.services

import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.config.schema
import io.vyne.cask.ddl.views.CaskViewService
import io.vyne.cask.upgrade.CaskSchemaChangeDetector
import io.vyne.cask.upgrade.CaskUpgradesRequiredEvent
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Schema
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.schemas.VersionedType
import io.vyne.schemas.fqn
import io.vyne.utils.log
import io.vyne.utils.orElse
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class CaskServiceBootstrap constructor(
   private val caskServiceSchemaGenerator: CaskServiceSchemaGenerator,
   private val schemaProvider: SchemaProvider,
   private val caskConfigRepository: CaskConfigRepository,
   private val caskViewService: CaskViewService,
   private val caskServiceRegenerationRunner: CaskServiceRegenerationRunner,
   private val changeDetector: CaskSchemaChangeDetector,
   private val eventPublisher: ApplicationEventPublisher) {

   @Volatile
   private var lastServiceGenerationSuccessful: Boolean = false

   // TODO Update cask service when type changes (e.g. new attributes added)
   @EventListener
   fun regenerateCasksOnSchemaChange(event: SchemaSetChangedEvent) {
      log().info("Schema changed, checking for upgrade work required")
      val casksNeedingUpgrading = changeDetector.markModifiedCasksAsRequiringUpgrading(event.newSchemaSet.schema)

      if (casksNeedingUpgrading.isNotEmpty()) {
         eventPublisher.publishEvent(CaskUpgradesRequiredEvent())
      }

      caskServiceRegenerationRunner.regenerate {
         // Look for cask views that need rebuilding
         val newCaskViewConfigs = this.generateCaskViews()
         val caskVersionedViewTypes = findTypesToRegister(newCaskViewConfigs.toMutableList())
         if (caskVersionedViewTypes.isNotEmpty()) {
            caskServiceSchemaGenerator.generateAndPublishServices(caskVersionedViewTypes)
         }
      }

      when {
         casksNeedingUpgrading.isNotEmpty() -> {
            log().info("Schema changed, cask services need to be updated!")
            regenerateCaskServicesAsync()
         }
         !lastServiceGenerationSuccessful -> {
            log().info("Last attempt to generate services failed.  The schema has changed, so will reattempt")
            regenerateCaskServicesAsync()
         }
         else -> {
            log().info("Upgrade check completed, nothing to do.")
         }
      }

   }

   @EventListener(value = [ContextRefreshedEvent::class])
   fun regenerateCaskServicesAsync() {
      caskServiceRegenerationRunner.regenerate { regenerateCaskServices() }
   }

   private fun generateCaskViews(): List<CaskConfig> {
      return caskViewService.generateViews()
   }

   private fun regenerateCaskServices() {
      this.generateCaskViews()
      val caskConfigs = caskConfigRepository.findAll()
      log().info("Total number of CaskConfig entries=${caskConfigs.size}")
      val caskVersionedTypes = findTypesToRegister(caskConfigs)
      if (caskVersionedTypes.isNotEmpty()) {
         caskServiceSchemaGenerator.generateAndPublishServices(caskVersionedTypes)
      }
   }

   private fun findTypesToRegister(caskConfigs: List<CaskConfig>): List<CaskTaxiPublicationRequest> {
      if (caskConfigs.isEmpty()) {
         return emptyList()
      }
      val schema = getSchema() ?: return emptyList()

      val caskPublicationRequests = caskConfigs.map { caskConfig ->
         try {
            if (caskConfig.exposesType) {
               val caskSchema = caskConfig.schema(importSchema = schema)
               val type = caskSchema.versionedType(caskConfig.qualifiedTypeName.fqn())
               CaskTaxiPublicationRequest(
                  type,
                  registerService = true,
                  registerType = true
               )
            } else {
               CaskTaxiPublicationRequest(
                  schema.versionedType(caskConfig.qualifiedTypeName.fqn()),
                  registerService = true,
                  registerType = false
               )
            }
         } catch (e: Exception) {
            log().error("Unable to find type ${caskConfig.qualifiedTypeName.fqn()}. Flagging cask generation failure, will try again on next schema update. Error: ${e.message}")
            lastServiceGenerationSuccessful = false
            null
         }
      }
      if (caskPublicationRequests.any { it == null }) {
         lastServiceGenerationSuccessful = false
      } else {
         if (!lastServiceGenerationSuccessful) {
            log().info("Poll for service generation requests completed successfully.  Flagging as healthy again")
            lastServiceGenerationSuccessful = true
         }
      }
      return caskPublicationRequests.filterNotNull()

   }

   private fun getSchema(): Schema? {
      return try {
         schemaProvider.schema()
      } catch (e: Exception) {
         log().error("Unable to read the schema. Possible compilation errors. Check the file-schema-server log for more details.", e)
         null
      }
   }
}

data class CaskTaxiPublicationRequest(
   val type: VersionedType,
   val registerService: Boolean = true,
   val registerType: Boolean = false
)

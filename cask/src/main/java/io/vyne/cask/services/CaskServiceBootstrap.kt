package io.vyne.cask.services

import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.config.schema
import io.vyne.cask.ddl.views.CaskViewService
import io.vyne.cask.ingest.IngestionEventHandler
import io.vyne.cask.ingest.IngestionInitialisedEvent
import io.vyne.cask.upgrade.CaskSchemaChangeDetector
import io.vyne.cask.upgrade.CaskUpgradesRequiredEvent
import io.vyne.schemaStore.ControlSchemaPollEvent
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Schema
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.schemas.VersionedType
import io.vyne.schemas.fqn
import io.vyne.utils.log
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

// This class needs refactoring and splitting out.
// It has poor test coverage, and too many responsibilities, but they
// seem fairly intertwined.
@Service
class CaskServiceBootstrap constructor(
   private val caskServiceSchemaGenerator: CaskServiceSchemaGenerator,
   private val schemaProvider: SchemaProvider,
   private val caskConfigRepository: CaskConfigRepository,
   private val caskViewService: CaskViewService,
   private val caskServiceRegenerationRunner: CaskServiceRegenerationRunner,
   private val changeDetector: CaskSchemaChangeDetector,
   private val ingestionEventHandler: IngestionEventHandler,
   private val eventPublisher: ApplicationEventPublisher) {

   @Volatile
   private var lastServiceGenerationSuccessful: Boolean = false

   @EventListener
   fun regenerateCasksOnSchemaChange(event: SchemaSetChangedEvent) {
      log().info("Schema changed, checking for upgrade work required")
      log().info("Looking for any active casks that require migrating")
      val taggedCasks = changeDetector.markModifiedCasksAsRequiringUpgrading(event.newSchemaSet.schema)
      if (taggedCasks.isNotEmpty()) {
         log().info("The following casks were newly tagged as needing migrating: ${taggedCasks.joinToString { it.config.qualifiedTypeName }}")
      }

      // Look for casks needing upgrading independent of the casks we just tagged.
      // This catches any migrations that were tagged in a previous startup attempt, but didn't migrate
      // completely.
      // This was added after db connection errors left partially migrated casks.
      val casksNeedingUpgrading = changeDetector.findCasksTaggedAsMigrating()

      if (casksNeedingUpgrading.isNotEmpty()) {
         log().info("The following casks were found as needing migrating: ${casksNeedingUpgrading.joinToString { it.qualifiedTypeName }}")
         // Stop polling for new schema changes till we finish upgrade operation.
         eventPublisher.publishEvent(ControlSchemaPollEvent(false))
         eventPublisher.publishEvent(CaskUpgradesRequiredEvent())
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

   @EventListener
   fun onIngesterInitialised(event: IngestionInitialisedEvent) {
      log().info("Received Ingestion Initialised event ${event.type}")
      // immediately create the configuration and corresponding data table.
      ingestionEventHandler.onIngestionInitialised(event)
      if (caskServiceSchemaGenerator.alreadyExists(event.type)) {
         log().info("Cask service ${CaskServiceSchemaGenerator.caskServiceSchemaName(event.type)} already exists ")
      } else {
         caskServiceSchemaGenerator.generateAndPublishService(CaskTaxiPublicationRequest(
            event.type,
            registerService = true,
            registerType = false
         ))
      }
   }

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

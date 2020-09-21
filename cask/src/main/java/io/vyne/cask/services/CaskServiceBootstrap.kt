package io.vyne.cask.services

import io.vyne.VersionedSource
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.config.schema
import io.vyne.cask.ddl.views.CaskViewService
import io.vyne.cask.services.CaskServiceSchemaGenerator.Companion.CaskNamespacePrefix
import io.vyne.cask.upgrade.CaskSchemaChangeDetector
import io.vyne.cask.upgrade.CaskUpgradesRequiredEvent
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemaStore.SchemaSet
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

   private var lastServiceGenerationSuccessful:Boolean = false

   // TODO Update cask service when type changes (e.g. new attributes added)
   @EventListener
   fun regenerateCasksOnSchemaChange(event: SchemaSetChangedEvent) {
      log().info("Schema changed, checking for upgrade work required")
      val casksNeedingUpgrading = changeDetector.markModifiedCasksAsRequiringUpgrading(event.newSchemaSet.schema)

      if (casksNeedingUpgrading.isNotEmpty()) {
         eventPublisher.publishEvent(CaskUpgradesRequiredEvent())
      }

      val caskChangesOnly = casksNeedingUpgrading.isNotEmpty() && casksNeedingUpgrading.all { it.config.qualifiedTypeName.startsWith(CaskNamespacePrefix) }
      when {
         caskChangesOnly -> {
            log().info("Schema changed, cask services do not need updating, but re-registering Cask Views.")
            // Required to handle the case:
            // - blank Cask DB and configuration with a view that depends on cask A and cask B
            // - Create Cask A
            // - Create Cask B
            // We need below call to handle view generation.
            caskServiceRegenerationRunner.regenerate {
               val newCaskViewConfigs = this.generateCaskViews()
               val caskVersionedViewTypes = findTypesToRegister(newCaskViewConfigs.toMutableList())
               if (caskVersionedViewTypes.isNotEmpty()) {
                  caskServiceSchemaGenerator.generateAndPublishServices(caskVersionedViewTypes)
               }
            }
         }
         casksNeedingUpgrading.isNotEmpty() -> {
            log().info("Schema changed, cask services need to be updated!")
            caskServiceRegenerationRunner.regenerate { regenerateCaskServices() }
         }
         !lastServiceGenerationSuccessful -> {
            log().info("Last attempt to generate services failed.  The schema has changed, so will reattempt")
            regenerateCaskServices()
         }
         else -> {
            log().info("Upgrade check completed, nothing to do.")
         }
      }

   }

   @EventListener(value = [ContextRefreshedEvent::class])
   fun generateCaskServicesOnStartup() {
      caskServiceRegenerationRunner.regenerate { regenerateCaskServices() }
   }

   private fun generateCaskViews(): List<CaskConfig> {
      return caskViewService.bootstrap()
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

   private fun findTypesToRegister(caskConfigs: MutableList<CaskConfig>): List<CaskTaxiPublicationRequest> {
      return getSchema()?.let { schema ->
         val caskPublicationRequests =  caskConfigs.map { caskConfig ->
            if (caskConfig.exposesType) {
               val caskSchema = caskConfig.schema(importSchema = schema)
               val type = caskSchema.versionedType(caskConfig.qualifiedTypeName.fqn())
               CaskTaxiPublicationRequest(
                  type,
                  registerService = true,
                  registerType = true
               )
            } else {
               try {
                  CaskTaxiPublicationRequest(
                     schema.versionedType(caskConfig.qualifiedTypeName.fqn()),
                     registerService = true,
                     registerType = false
                  )
               } catch (e: Exception) {
                  log().error("Unable to find type ${caskConfig.qualifiedTypeName.fqn()}. Flagging cask generation failure, will try again on next schema update. Error: ${e.message}")
                  lastServiceGenerationSuccessful = false
                  null
               }
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
         caskPublicationRequests.filterNotNull()
      }?.toList().orElse(emptyList())
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

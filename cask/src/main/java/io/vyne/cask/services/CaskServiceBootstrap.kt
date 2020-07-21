package io.vyne.cask.services

import io.vyne.VersionedSource
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.query.CaskDAO
import io.vyne.cask.services.CaskServiceSchemaGenerator.Companion.CaskNamespacePrefix
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Schema
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.schemas.VersionedType
import io.vyne.schemas.fqn
import io.vyne.utils.log
import io.vyne.utils.orElse
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class CaskServiceBootstrap constructor(
   private val caskServiceSchemaGenerator: CaskServiceSchemaGenerator,
   private val schemaProvider: SchemaProvider,
   private val caskDAO: CaskDAO) {

   // TODO Update cask service when type changes (e.g. new attributes added)
   @EventListener
   fun regenerateCasksOnSchemaChange(event: SchemaSetChangedEvent) {
      val oldSchemas: Set<VersionedSource> = event.oldSchemaSet?.allSources?.toSet().orElse(emptySet())
      val newSchemas: Set<VersionedSource> = event.newSchemaSet.allSources.toSet()

      val deleted = oldSchemas.subtract(newSchemas)
      log().info("Deleted schemas: ${deleted.map { it.id }.sorted()}")

      val added = newSchemas.subtract(oldSchemas)
      log().info("Added schemas: ${added.map { it.id }.sorted()}")

      val caskChangesOnly = (deleted + added).all { it.name.startsWith(CaskNamespacePrefix) }
      if (caskChangesOnly) {
         log().info("Schema changed, cask services do not need updating.")
         return
      }

      log().info("Schema changed, cask services need to be updated!")
      regenerateCaskServices()
   }

   @EventListener(value = [ContextRefreshedEvent::class])
   fun generateCaskServicesOnStartup() {
      regenerateCaskServices()
   }

   private fun regenerateCaskServices() {
      val caskConfigs = caskDAO.findAllCaskConfigs()
      log().info("Total number of CaskConfig entries=${caskConfigs.size}")
      val caskVersionedTypes = findExistingCaskTypes(caskConfigs)
      if (caskVersionedTypes.isNotEmpty()) {
         caskServiceSchemaGenerator.generateAndPublishServices(caskVersionedTypes)
      }
   }

   private fun findExistingCaskTypes(caskConfigs: MutableList<CaskConfig>): List<VersionedType> {
      return getSchema()?.let { schema ->
         caskConfigs.mapNotNull {
            try {
               schema.versionedType(it.qualifiedTypeName.fqn())
            } catch (e: Exception) {
               log().error("Unable to find type ${it.qualifiedTypeName.fqn()} Error: ${e.message}")
               null
            }
         }
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

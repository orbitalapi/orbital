package io.vyne.cask.services

import io.vyne.cask.query.CaskDAO
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Schema
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.schemas.fqn
import io.vyne.utils.log
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class CaskServiceBootstrap constructor(
   private val caskServiceSchemaGenerator: CaskServiceSchemaGenerator,
   private val schemaProvider: SchemaProvider,
   private val caskDAO: CaskDAO) {

   // TODO Update cask service when type changes (e.g. new attributes added)
   // TODO Update schema in one batch so that it triggers schema update only once!
   // TODO This logic fails when we are not connected to eureka and we don't have schema populated
   // Possible solution is to wait for application start and schema populated/updated event
   @EventListener(value = [ContextRefreshedEvent::class, SchemaSetChangedEvent::class])
   fun initializeCaskServices() {
      val caskConfigs = caskDAO.findAllCaskConfigs()
      log().info("Number of CaskConfig entries=${caskConfigs.size}")
      getSchema()?.let { schema ->
         caskConfigs.forEach {
            try {
               val versionedType = schema.versionedType(it.qualifiedTypeName.fqn())
               log().info("Initializing service for type=${it.qualifiedTypeName} tableName=${it.tableName}")
               caskServiceSchemaGenerator.generateAndPublishService(versionedType)
            } catch (e: Exception) {
               log().error("Service initialization Error: ${e.message}")
            }
         }
      }
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

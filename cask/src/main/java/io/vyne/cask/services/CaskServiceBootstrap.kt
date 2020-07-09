package io.vyne.cask.services

import io.vyne.cask.api.CaskConfig
import io.vyne.cask.ddl.caskRecordTable
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
      log().info("Total number of CaskConfig entries=${caskConfigs.size}")
      val caskConfigsGroupedByQualifiedName = caskConfigs.groupBy { it.qualifiedTypeName }
      caskConfigsGroupedByQualifiedName.forEach { (qualifiedTypeName, configs) ->

         log().info("Type=${qualifiedTypeName}, CaskConfig entries=${configs.size}")
         val latestCaskConfig = configs.sortedByDescending { it.insertedAt }.first()

         latestCaskConfig?.let {
            initializeServiceOperations(qualifiedTypeName, it)
         }
      }
   }

   private fun initializeServiceOperations(qualifiedTypeName: String, caskConfig: CaskConfig) {
      try {
         val currentVersionedType = schemaProvider.schema().versionedType(qualifiedTypeName.fqn())
         val currentTableName = currentVersionedType.caskRecordTable()

         if (caskConfig.tableName != currentTableName) {
            log().info("Type=${qualifiedTypeName} has changed. Creating empty cask table ${currentTableName}.")
            caskDAO.createCaskConfig(currentVersionedType)
            caskDAO.createCaskRecordTable(currentVersionedType)
         }
         log().info("Type=${qualifiedTypeName} tableName=${currentTableName} initializing service operations.")
         caskServiceSchemaGenerator.generateAndPublishService(currentVersionedType)
      } catch (e: Exception) {
         log().error("Type=${qualifiedTypeName} service initialization Error: ${e.message}")
      }
   }
}

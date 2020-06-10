package io.vyne.cask.services

import io.vyne.cask.query.CaskDAO
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.fqn
import io.vyne.utils.log
import org.springframework.stereotype.Service

@Service
class CaskServiceBootstrap constructor(
   private val caskServiceSchemaGenerator: CaskServiceSchemaGenerator,
   private val schemaProvider: SchemaProvider,
   private val caskDAO: CaskDAO) {

   init {
      initializeCaskServices()
   }

   // TODO Update cask service when type changes (e.g. new attributes added)

   private fun initializeCaskServices() {
      val caskConfigs = caskDAO.findAllCaskConfigs()
      log().info("Number of CaskConfig entries=${caskConfigs.size}")
      val schema = schemaProvider.schema()
      caskConfigs.forEach {
         val versionedType = schema.versionedType(it.qualifiedTypeName.fqn())
         log().info("Initializing service for type=${it.qualifiedTypeName} tableName=${it.tableName}")
         caskServiceSchemaGenerator.generateAndPublishService(versionedType)
      }
   }
}

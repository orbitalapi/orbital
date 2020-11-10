package io.vyne.cask.upgrade

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.api.CaskStatus
import io.vyne.cask.api.ContentType
import io.vyne.cask.api.CsvIngestionParameters
import io.vyne.cask.api.XmlIngestionParameters
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.format.json.JsonStreamSource
import io.vyne.cask.ingest.CaskIngestionErrorProcessor
import io.vyne.cask.ingest.IngesterFactory
import io.vyne.cask.ingest.IngestionStream
import io.vyne.cask.ingest.StreamSource
import io.vyne.cask.query.CaskDAO
import io.vyne.cask.websocket.CsvWebsocketRequest
import io.vyne.cask.websocket.XmlWebsocketRequest
import io.vyne.models.json.Jackson
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.VersionedType
import io.vyne.schemas.fqn
import io.vyne.utils.log
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.io.InputStream

@Component
class CaskUpgraderService(private val caskDAO: CaskDAO,
                          private val schemaProvider: SchemaProvider,
                          private val ingesterFactory: IngesterFactory,
                          private val configRepository: CaskConfigRepository,
                          private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
                          private val applicationEventPublisher: ApplicationEventPublisher,
                          private val caskIngestionErrorProcessor: CaskIngestionErrorProcessor
) {
   fun upgradeAll(casks:List<CaskNeedingUpgrade>) {
      casks.forEach { upgrade(it.config) }
   }
   fun upgrade(config: CaskConfig) {
      log().info("Starting to upgrade cask ${config.tableName}")
      val schema = try {
          schemaProvider.schema()
      } catch (exception:Exception) {
         log().warn("Unable to upgrade cask ${config.tableName}, as the schema is invalid.  Will try later", exception)
         return
      }

      // TODO : Possible issue here that the versioned type doesn't match the
      // type we're upgrading to, because time has passed between the config being
      // tagged for upgrade, and now.
      val targetType = schema.versionedType(config.qualifiedTypeName.fqn())
      val messageIds = caskDAO.findMessageIdsToReplay(config.tableName, config.replacedByTableName!!)
      // Window size of 50 is arbitary, and we can tune later
      Flux.fromIterable(messageIds)
         .buffer(50)
         .flatMapIterable { batchOfIds ->
            val messages = caskDAO.getCaskMessages(batchOfIds)
            messages
         }
         .filter { caskMessage ->
            when {
               caskMessage.messageContentType == null -> {
                  log().warn("Cannot reprocess message ${caskMessage.messageContentId} as content type is not known")
                  false
               }
               caskMessage.messageContentId == null -> {
                  log().warn("Cannot reprocess message ${caskMessage.messageContentId} as it's content was not stored")
                  false
               }
               else -> true
            }
         }
         .map { caskMessage ->
            val inputStream = caskDAO.getMessageContent(caskMessage.messageContentId!!)
            val streamSource = buildStreamSource(caskMessage.messageContentType!!, targetType, caskMessage.id, inputStream, caskMessage.ingestionParams)
            IngestionStream(
               targetType,
               TypeDbWrapper(targetType, schema),
               streamSource)

         }
         .filter { it != null }
         .map { it!! }
         .doOnComplete {
            log().info("Upgrade of cask table ${config.tableName} is complete, marking as replaced")
            configRepository.save(config.copy(status = CaskStatus.REPLACED))
            applicationEventPublisher.publishEvent(CaskUpgradeCompletedEvent(config.tableName))
         }
         .subscribe { ingestionStream: IngestionStream ->
            ingesterFactory
               .create(ingestionStream)
               .ingest()
               .subscribe()
         }
   }

   private fun buildStreamSource(contentType: ContentType, versionedType: VersionedType, messageId: String, inputStream: Flux<InputStream>, messageParams: String?): StreamSource {
      return when (contentType) {
         ContentType.json -> JsonStreamSource(
            inputStream,
            versionedType,
            schemaProvider.schema(),
            messageId,
            objectMapper
         )
         ContentType.csv -> {
            val csvIngestionParameters = tryParseCsvMessageParams(messageParams)
            CsvWebsocketRequest(csvIngestionParameters, versionedType, caskIngestionErrorProcessor)
               .buildStreamSource(inputStream, versionedType, schemaProvider.schema(), messageId)
         }
         ContentType.xml -> {
            val parameters = tryParseXmlMessageParams(messageParams)
            XmlWebsocketRequest(parameters, versionedType).buildStreamSource(inputStream, versionedType, schemaProvider.schema(), messageId)
         }
      }
   }

   private fun tryParseCsvMessageParams(messageParams: String?): CsvIngestionParameters {
      return if (messageParams == null) {
         log().warn("No message parameters were stored, will attempt to use defaults")
         CsvIngestionParameters()
      } else {
         try {
            objectMapper.readValue<CsvIngestionParameters>(messageParams)
         } catch (e: Exception) {
            log().warn("Failed to parse ingestionParams, will attempt to use defaults. $messageParams", e)
            CsvIngestionParameters()
         }
      }
   }

   private fun tryParseXmlMessageParams(messageParams: String?): XmlIngestionParameters {
      return if (messageParams == null) {
         log().warn("No message parameters were stored, will attempt to use defaults")
         XmlIngestionParameters()
      } else {
         try {
            objectMapper.readValue<XmlIngestionParameters>(messageParams)
         } catch (e: Exception) {
            log().warn("Failed to parse XmlIngestionParameters, will attempt to use defaults. $messageParams", e)
            XmlIngestionParameters()
         }
      }
   }
}

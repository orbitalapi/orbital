package io.vyne.cask.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.cask.CaskIngestionRequest
import io.vyne.cask.api.ContentType
import io.vyne.cask.api.CsvIngestionParameters
import io.vyne.cask.api.JsonIngestionParameters
import io.vyne.cask.api.XmlIngestionParameters
import io.vyne.cask.api.csv.CsvFormatFactory
import io.vyne.cask.format.csv.CsvStreamSource
import io.vyne.cask.format.json.JsonStreamSource
import io.vyne.cask.format.xml.XmlStreamSource
import io.vyne.cask.ingest.CaskIngestionErrorProcessor
import io.vyne.cask.ingest.StreamSource
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import reactor.core.publisher.Flux
import java.io.InputStream

data class JsonWebsocketRequest(
   override val parameters: JsonIngestionParameters,
   override val versionedType: VersionedType,
   private val objectMapper: ObjectMapper
) : CaskIngestionRequest {
   override val contentType = ContentType.json
   override val debug: Boolean = parameters.debug
   override val nullValues: Set<String> = emptySet()

   override fun buildStreamSource(input: Flux<InputStream>, type: VersionedType, schema: Schema,messageId: String): StreamSource {
      return JsonStreamSource(
         input,
         versionedType,
         schema,
         messageId,
         objectMapper
      )
   }
}


data class CsvWebsocketRequest(
   override val parameters: CsvIngestionParameters,
   override val versionedType: VersionedType,
   private val caskIngestionErrorProcessor: CaskIngestionErrorProcessor
) : CaskIngestionRequest {
   override val contentType = ContentType.csv
   override val debug: Boolean = parameters.debug
   override val nullValues: Set<String> = parameters.nullValue
   val csvFormat by lazy {
      CsvFormatFactory.fromParameters(parameters)
   }
   val ignoreContentBefore = parameters.ignoreContentBefore

   override fun buildStreamSource(input: Flux<InputStream>, type: VersionedType, schema: Schema, messageId:String): StreamSource {
      return CsvStreamSource(
         input, type, schema,
         messageId = messageId,
         csvFormat = this.csvFormat,
         nullValues = parameters.nullValue,
         ignoreContentBefore = parameters.ignoreContentBefore,
         ingestionErrorProcessor = caskIngestionErrorProcessor
      )
   }
}

data class XmlWebsocketRequest(
   override val parameters: XmlIngestionParameters,
   override val versionedType: VersionedType
) : CaskIngestionRequest {
   override val contentType = ContentType.xml
   override val debug: Boolean = parameters.debug
   override val nullValues: Set<String> = emptySet()
   override fun buildStreamSource(input: Flux<InputStream>, type: VersionedType, schema: Schema, messageId:String): StreamSource {
      return XmlStreamSource(input, type, schema, messageId, parameters.elementSelector)
   }
}

package io.vyne.cask.websocket

import arrow.core.Either
import arrow.core.flatMap
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.cask.CaskIngestionRequest
import io.vyne.cask.CaskService
import io.vyne.cask.format.csv.CsvStreamSource
import io.vyne.cask.format.json.JsonStreamSource
import io.vyne.cask.ingest.StreamSource
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import io.vyne.utils.orElse
import org.apache.commons.csv.CSVFormat
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import java.io.InputStream
import java.nio.file.Path

abstract class CaskWebsocketRequest : CaskIngestionRequest {
   abstract val session: WebSocketSession
   val params: MultiValueMap<String, String?>?
        get() = session.queryParams()

   fun debug() : Boolean = params?.getParam("debug").orElse("false").toBoolean()
   fun nullValues() : Set<String> = params?.getParams("nullValue").orElse(emptyList<String>())
      .filterNotNull()
      .toSet()

   companion object {
      @JvmStatic
      fun create(session: WebSocketSession,
         contentType: CaskService.ContentType,
                 versionedType: VersionedType,
                 objectMapper: ObjectMapper): CaskWebsocketRequest {
         return when (contentType) {
            CaskService.ContentType.json -> JsonWebsocketRequest(session, versionedType, objectMapper)
            CaskService.ContentType.csv -> CsvWebsocketRequest(session, versionedType)
         }
               }
            }
      }

data class JsonWebsocketRequest(override val session: WebSocketSession, override val versionedType: VersionedType, private val objectMapper: ObjectMapper) : CaskWebsocketRequest() {
   override fun buildStreamSource(input: Flux<InputStream>, type: VersionedType, schema: Schema, readCacheDirectory: Path): StreamSource {
      return JsonStreamSource(
         input,
         versionedType,
         schema,
         readCacheDirectory,
         objectMapper
      )
   }
}

data class CsvWebsocketRequest(override val session: WebSocketSession, override val versionedType: VersionedType) : CaskWebsocketRequest() {
   fun csvFormat(): CSVFormat {
      val csvDelimiter: Char = params?.getParam("csvDelimiter").orElse(",").single()
      val csvFirstRecordAsHeader: Boolean = params?.getParam("csvFirstRecordAsHeader").orElse("true").toBoolean()
      val format = CSVFormat.DEFAULT.withDelimiter(csvDelimiter)
      if (csvFirstRecordAsHeader) {
         return format.withFirstRecordAsHeader()
      }
      return format
   }

   override fun buildStreamSource(input: Flux<InputStream>, type: VersionedType, schema: Schema, readCacheDirectory: Path): StreamSource {
      return CsvStreamSource(
         input, type, schema, readCacheDirectory,
         csvFormat = this.csvFormat(),
         nullValues = this.nullValues()
      )
   }
}


data class CsvIngestionRequest(val format: CSVFormat, override val versionedType: VersionedType, val nullValues: Set<String>) : CaskIngestionRequest {

   override fun buildStreamSource(input: Flux<InputStream>, type: VersionedType, schema: Schema, readCacheDirectory: Path): StreamSource {
      return CsvStreamSource(
         input,
         type, schema, readCacheDirectory,
         csvFormat = format,
         nullValues = nullValues
      )
   }
}

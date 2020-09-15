package io.vyne.cask.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.cask.CaskIngestionRequest
import io.vyne.cask.api.ContentType
import io.vyne.cask.api.CsvIngestionParameters
import io.vyne.cask.api.JsonIngestionParameters
import io.vyne.cask.api.csv.CsvFormatFactory
import io.vyne.cask.format.csv.CsvStreamSource
import io.vyne.cask.format.json.JsonStreamSource
import io.vyne.cask.ingest.StreamSource
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import org.apache.commons.csv.CSVFormat
import reactor.core.publisher.Flux
import java.io.InputStream
import java.nio.file.Path

data class JsonWebsocketRequest(private val params: JsonIngestionParameters, override val versionedType: VersionedType, private val objectMapper: ObjectMapper) : CaskIngestionRequest {
   override val contentType = ContentType.json
   override val debug: Boolean = params.debug
   override val nullValues: Set<String> = emptySet()

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


data class CsvWebsocketRequest(private val parameters: CsvIngestionParameters, override val versionedType: VersionedType) : CaskIngestionRequest {
   override val contentType = ContentType.csv
   override val debug: Boolean = parameters.debug
   override val nullValues: Set<String> = parameters.nullValue
   val csvFormat by lazy {
      CsvFormatFactory.fromParameters(parameters)
   }
   val ignoreContentBefore = parameters.ignoreContentBefore

   override fun buildStreamSource(input: Flux<InputStream>, type: VersionedType, schema: Schema, readCacheDirectory: Path): StreamSource {
      return CsvStreamSource(
         input, type, schema, readCacheDirectory,
         csvFormat = this.csvFormat,
         nullValues =  parameters.nullValue,
         ignoreContentBefore = parameters.ignoreContentBefore
      )
   }
}

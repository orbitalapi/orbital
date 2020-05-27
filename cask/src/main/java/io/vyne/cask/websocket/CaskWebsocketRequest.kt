package io.vyne.cask.websocket

import arrow.core.Either
import arrow.core.flatMap
import io.vyne.cask.CaskService
import io.vyne.schemas.VersionedType
import io.vyne.utils.orElse
import org.apache.commons.csv.CSVFormat
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.socket.WebSocketSession

abstract class CaskWebsocketRequest {
   data class CaskWebsocketRequestError(val message: String)
   abstract val session: WebSocketSession
   abstract val versionedType: VersionedType
   val params: MultiValueMap<String, String?>?
        get() = session.queryParams()

   fun debug() : Boolean = params?.getParam("debug").orElse("false").toBoolean()
   fun nullValues() : Set<String> = params?.getParams("nullValue").orElse(emptyList<String>())
      .filterNotNull()
      .toSet()

   companion object {
      @JvmStatic
      fun create(session: WebSocketSession,
                 contentType: Either<CaskService.ContentTypeError, CaskService.ContentType>,
                 versionedType: Either<CaskService.TypeError, VersionedType>): Either<CaskWebsocketRequestError, CaskWebsocketRequest> {
         return contentType
            .mapLeft { error -> CaskWebsocketRequestError(error.message) }
            .map { handleType(versionedType, it, session) }
            .flatMap { it }
      }

      private fun handleType(
         versionedType: Either<CaskService.TypeError, VersionedType>,
         contentType: CaskService.ContentType,
         session: WebSocketSession): Either<CaskWebsocketRequestError, CaskWebsocketRequest> {
         return versionedType
            .mapLeft { type ->  CaskWebsocketRequestError(type.message)}
            .map {type ->
               when (contentType) {
                  CaskService.ContentType.json -> JsonWebsocketRequest(session, type)
                  CaskService.ContentType.csv -> CsvWebsocketRequest(session, type)
               }
            }
      }
   }
}

data class JsonWebsocketRequest(override val session: WebSocketSession, override val versionedType: VersionedType) : CaskWebsocketRequest()
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
}

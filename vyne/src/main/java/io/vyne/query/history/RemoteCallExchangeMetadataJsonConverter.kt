package io.vyne.query.history

import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.query.RemoteCallExchangeMetadata


object RemoteCallExchangeMetadataJsonConverter : JsonConverter<RemoteCallExchangeMetadata>() {

   override fun fromJson(json: String): RemoteCallExchangeMetadata {
      return objectMapper.readValue(json)
   }
}

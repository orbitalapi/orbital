package com.orbitalhq.query.history

import com.fasterxml.jackson.module.kotlin.readValue
import com.orbitalhq.query.RemoteCallExchangeMetadata


object RemoteCallExchangeMetadataJsonConverter : JsonConverter<RemoteCallExchangeMetadata>() {

   override fun fromJson(json: String): RemoteCallExchangeMetadata {
      return objectMapper.readValue(json)
   }
}

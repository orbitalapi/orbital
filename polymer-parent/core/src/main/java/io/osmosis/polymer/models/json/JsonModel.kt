package io.osmosis.polymer.models.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.osmosis.polymer.Model

class JsonModel(val json: String, override val typeName: String, val objectMapper: ObjectMapper = jacksonObjectMapper()) : Model {
}

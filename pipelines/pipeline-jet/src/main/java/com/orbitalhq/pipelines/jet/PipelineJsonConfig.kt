package com.orbitalhq.pipelines.jet

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.pipelines.jet.api.transport.PipelineJacksonModule

object PipelineJsonConfig {
   fun newMapper(): ObjectMapper {
      return Jackson.newObjectMapperWithDefaults()
         .registerModule(PipelineJacksonModule())
   }

   fun lenientReadingMapper() = newMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

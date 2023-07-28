package io.vyne.pipelines.jet

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.models.json.Jackson
import io.vyne.pipelines.jet.api.transport.PipelineJacksonModule

object PipelineJsonConfig {
   fun newMapper(): ObjectMapper {
      return Jackson.newObjectMapperWithDefaults()
         .registerModule(PipelineJacksonModule())
   }

   fun lenientReadingMapper() = newMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

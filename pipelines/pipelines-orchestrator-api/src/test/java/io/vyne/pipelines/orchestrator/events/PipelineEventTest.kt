package io.vyne.pipelines.orchestrator.events

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winterbe.expekt.should
import org.junit.Test
import java.time.Instant

class PipelineEventTest {
   val jackson = jacksonObjectMapper()
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,false)
      .registerModule(JavaTimeModule())

   @Test
   fun canSerializeLogMessageEvent() {
      val event = PipelineMessageEvent(
         "jobId",
         "stageName",
         PipelineMessage(
            PipelineMessage.Severity.DEBUG,
            Instant.now(),
            "Hello, world"
         )
      )

      val json = jackson.writerWithDefaultPrettyPrinter().writeValueAsString(event)
      val deserialized = jackson.readValue<PipelineEvent>(json)
      require(deserialized is PipelineMessageEvent)
      event.should.equal(deserialized)
   }

   @Test
   fun canSerializeLifecycleEvent() {
      val event = PipelineStageEvent(
         "jobId",
         "stageName",
         Instant.now(),
         PipelineStageStatus.RUNNING
      )

      val json = jackson.writerWithDefaultPrettyPrinter().writeValueAsString(event)
      val deserialized = jackson.readValue<PipelineStageEvent>(json)
      require(deserialized is PipelineStageEvent)
      event.should.equal(deserialized)

   }
}

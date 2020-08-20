package io.vyne.pipelines.orchestrator

import com.fasterxml.jackson.databind.ObjectMapper
import com.winterbe.expekt.should
import io.vyne.pipelines.orchestrator.configuration.PipelineConfigurationProperties
import io.vyne.pipelines.orchestrator.pipelines.PipelinesService
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner


@RunWith(SpringRunner::class)
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "eureka.client.enabled=false"
   ])
@ActiveProfiles(profiles = ["test"])
class PipelineConfigurationPropertiesTest {
   @Autowired
   lateinit var pipelineConfigurationProperties: PipelineConfigurationProperties
   @Autowired
   lateinit var objectMapper: ObjectMapper
   @Autowired
   lateinit var pipelineService: PipelinesService

   @Test
   fun `Pipeline definitions can be passed in yml or from a dedicated file`() {
      val pipelines = pipelineService.pipelines()
      pipelines.size.should.equal(4)
      pipelines[0].name.should.equal("test-pipeline-1")
      pipelines[1].name.should.equal("test-pipeline-2")
      pipelines[2].name.should.equal("test-pipeline-3")
      pipelines[3].name.should.equal("test-pipeline-4")

      val stripWhiteSpaces = Regex("\\s")
      pipelines[0].pipelineDescription.replace(stripWhiteSpaces, "").should.equal("""
         {"name":"test-pipeline-1",
         "input":{
               "type":"imdb.Actor",
               "transport":{
                  "topic":"pipeline-input-avro",
                  "targetType":"imdb.Actor",
                  "type":"kafka",
                  "direction":"INPUT",
                  "props":{
                     "group.id":"vyne-pipeline-group",
                     "bootstrap.servers":"kafka:9092,",
                     "heartbeat.interval.ms":"3000",
                     "session.timeout.ms":"10000",
                     "auto.offset.reset":"earliest"
                     }
                  }
              },
        "output":{
               "type":"imdb.Actor",
               "transport":{
                  "props": {
                     "foo": "bar"
                  },
                  "targetType":"imdb.Actor",
                  "type":"cask",
                  "direction":"OUTPUT"
                 }
               }
      }
      """.trimIndent().replace(stripWhiteSpaces, ""))
   }
}

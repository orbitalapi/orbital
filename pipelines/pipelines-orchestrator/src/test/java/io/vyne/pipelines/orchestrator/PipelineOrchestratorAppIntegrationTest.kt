package io.vyne.pipelines.orchestrator


import com.nhaarman.mockito_kotlin.any
import com.winterbe.expekt.should
import io.vyne.pipelines.orchestrator.runners.PipelineRunnerApi
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit4.SpringRunner


@RunWith(SpringRunner::class)
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "spring.main.allow-bean-definition-overriding=true",
      "eureka.client.enabled=false"
   ])
@AutoConfigureMockMvc
class PipelineOrchestratorAppIntegrationTest {

   @Autowired
   private lateinit var restTemplate: TestRestTemplate;

   @MockBean
   private lateinit var runner: PipelineRunnerApi;

   @Test
   fun testValidPipelineDescriptionIngestion() {

      var pipelineDescription = pipelineDescription("kafka", "cask")
      given(runner.submitPipeline(any())).willReturn(pipelineDescription);

      var response = postPipeline(pipelineDescription)

      response.statusCode.should.equal(HttpStatus.OK)
      response.body.should.equal(pipelineDescription)

   }

   @Test
   fun testValidPipelineDescriptionIngestionUnknownType() {

      var pipelineDescription = pipelineDescription("XXX", "YYY")
      given(runner.submitPipeline(any())).willReturn(pipelineDescription);

      var response = postPipeline(pipelineDescription)

      response.statusCode.should.equal(HttpStatus.OK)
      response.body.should.equal(pipelineDescription)

   }

   @Test
   fun testInvalidPipelineDescriptionIngestion() {

      given(runner.submitPipeline(any())).willReturn(INVALID_PIPELINE_DESCRIPTION);

      var response = postPipeline(INVALID_PIPELINE_DESCRIPTION)

      response.statusCode.should.equal(HttpStatus.BAD_REQUEST)
      response.body.should.be.not.`null`
   }

   /**
    * Convenient method to post a pipeline through the TestRestTemplate
    */
   private fun postPipeline(pipelineDescription: String) = restTemplate.postForEntity("/runner/pipelines", pipelineDescription, String::class.java)

   private fun pipelineDescription(inputType: String, outputType: String) = VALID_PIPELINE_DESCRIPTION.format(inputType, outputType)

}

private var VALID_PIPELINE_DESCRIPTION = """
         {
           "name" : "test-pipeline",
           "input" : {
             "type" : "imdb.Actor",
             "transport" : {
               "topic" : "pipeline-input-avro",
               "targetType" : "imdb.Actor",
               "type" : "%s",
               "direction" : "INPUT",
               "props" : {
                  "group.id" : "vyne-pipeline-group",
                  "bootstrap.servers" : "kafka:9092,",
                  "heartbeat.interval.ms" : "3000",
                  "session.timeout.ms" : "10000",
                  "auto.offset.reset" : "earliest"
               }
             }
           },
           "output" : {
             "type" : "imdb.Actor",
             "transport" : {
               "props" : {
               },
               "targetType" : "imdb.Actor",
               "type" : "%s",
               "direction" : "OUTPUT"
             }
           }
         }
      """.trimIndent();

private var INVALID_PIPELINE_DESCRIPTION = """
         {
           "name" : "test-pipeline",
           "input" : {
             "type" : "imdb.Actor",
             "transport" : {
               "topic" : "pipeline-input-avro",
               "targetType" : "imdb.Actor"
             }
           },
           "output" : {
             "type" : "imdb.Actor"
           }
         }
      """.trimIndent();

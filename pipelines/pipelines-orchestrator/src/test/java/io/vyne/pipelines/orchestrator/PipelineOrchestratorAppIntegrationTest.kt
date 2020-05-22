package io.vyne.pipelines.orchestrator


import com.nhaarman.mockito_kotlin.any
import com.winterbe.expekt.should
import io.vyne.VersionedTypeReference
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineChannel
import io.vyne.pipelines.orchestrator.runners.PipelineRunnerApi
import io.vyne.pipelines.runner.PipelineInstanceReference
import io.vyne.pipelines.runner.SimplePipelineInstance
import io.vyne.pipelines.runner.transport.cask.CaskTransportOutputSpec
import io.vyne.pipelines.runner.transport.kafka.KafkaTransportInputSpec
import io.vyne.schemas.fqn
import org.junit.Before
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
import java.time.Instant


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

   @Autowired
   private lateinit var pipelinesManager: PipelinesManager

   @Before
   fun setup() {
      pipelinesManager.pipelines.clear()

      val pipeline = Pipeline(
         "test-pipeline",
         PipelineChannel(
            VersionedTypeReference("PersonLoggedOnEvent".fqn()),
            KafkaTransportInputSpec(topic = "pipeline-input", targetType = VersionedTypeReference("PersonLoggedOnEvent".fqn()), props = mapOf("key1" to "value1")
            )
         ),
         PipelineChannel(
            VersionedTypeReference("UserEvent".fqn()),
            CaskTransportOutputSpec(targetType = VersionedTypeReference("UserEvent".fqn()),  props = mapOf("key1" to "value1")
            )
         )
      )
      var pipelineReference = SimplePipelineInstance(pipeline, Instant.now())

      given(runner.submitPipeline(any(), any())).willReturn(pipelineReference);
   }

   @Test
   fun testValidPipelineDescriptionIngestion() {

      var pipelineDescription = pipelineDescription("kafka", "cask")

      var response = postPipeline(pipelineDescription, Pipeline::class.java)

      response.statusCode.should.equal(HttpStatus.OK)
      response.body.name.should.be.equal("test-pipeline")
      response.body.input.transport.type.should.be.equal("kafka")
      response.body.output.transport.type.should.be.equal("cask")
   }

   @Test
   fun testValidPipelineDescriptionIngestionUnknownType() {

      var pipelineDescription = pipelineDescription("XXX", "YYY")

      var response = postPipeline(pipelineDescription, Pipeline::class.java)

      response.statusCode.should.equal(HttpStatus.OK)
      response.body.name.should.be.equal("test-pipeline")
   }

   @Test
   fun testInvalidPipelineDescriptionIngestion() {


      var response = postPipeline(INVALID_PIPELINE_DESCRIPTION, String::class.java)

      response.statusCode.should.equal(HttpStatus.BAD_REQUEST)
      response.body.should.be.not.`null`
   }

   /**
    * Convenient method to post a pipeline through the TestRestTemplate
    */
   private fun <T> postPipeline(pipelineDescription: String, clazz: Class<T>) = restTemplate.postForEntity("/runner/pipelines", pipelineDescription, clazz)

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

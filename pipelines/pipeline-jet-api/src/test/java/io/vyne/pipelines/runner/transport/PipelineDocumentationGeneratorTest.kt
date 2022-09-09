package io.vyne.pipelines.runner.transport

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.io.Resources
import io.vyne.pipelines.jet.api.documentation.PipelineDocs
import io.vyne.pipelines.jet.api.documentation.PipelineParam
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.aws.s3.AwsS3TransportInputSpec
import io.vyne.pipelines.jet.api.transport.aws.s3.AwsS3TransportOutputSpec
import io.vyne.pipelines.jet.api.transport.aws.sqss3.AwsSqsS3TransportInputSpec
import io.vyne.pipelines.jet.api.transport.cask.CaskTransportOutputSpec
import io.vyne.pipelines.jet.api.transport.http.PollingTaxiOperationInputSpec
import io.vyne.pipelines.jet.api.transport.http.TaxiOperationOutputSpec
import io.vyne.pipelines.jet.api.transport.jdbc.JdbcTransportOutputSpec
import io.vyne.pipelines.jet.api.transport.kafka.KafkaTransportInputSpec
import io.vyne.pipelines.jet.api.transport.kafka.KafkaTransportOutputSpec
import io.vyne.pipelines.jet.api.transport.query.PollingQueryInputSpec
import io.vyne.utils.orElse
import lang.taxi.utils.log
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

class PipelineDocumentationGeneratorTest {

   private val documentedPipelineSpecs = listOf(
      AwsS3TransportInputSpec::class,
      AwsSqsS3TransportInputSpec::class,
      CaskTransportOutputSpec::class,
      PollingTaxiOperationInputSpec::class,
      PollingQueryInputSpec::class,
      TaxiOperationOutputSpec::class,
      KafkaTransportInputSpec::class,
      KafkaTransportOutputSpec::class,
      JdbcTransportOutputSpec::class,
      AwsS3TransportOutputSpec::class
   )

   fun docPath(fileName: String): Path {
      val currentPath = Paths.get(".").toRealPath()
      val pipelinesPathIndex = currentPath
         .indexOf(Paths.get("pipelines"))
      // Returns the root of the project
      val projectPart = currentPath.subpath(0, pipelinesPathIndex).toString()
      val projectRootPath = Paths.get(currentPath.root.toString(), projectPart)
      val docsPath = projectRootPath.resolve("docs/content/reference/pipelines-2.0/")
      return docsPath.resolve("$fileName.mdx")
   }


   @Test
   fun `generate docs`() {
      var template = Resources.getResource("pipeline-specs.mdx")
         .readText()
      val generatedHeaderWarning = """---
         |IMPORTANT: This file is generated.  Do not edit manually.  For the preamble, edit pipeline-specs.mdx in pipeline-jet-api/src/test/resource. All other content is generated directly from classes
         |
      """.trimMargin()
      template = template.replaceFirst("---", generatedHeaderWarning)
      val docs = DocumentationWriter()
         .generateFor(documentedPipelineSpecs)

      val file = docPath("pipeline-specs").toFile()
      file.writeText(template + docs)
      log().info("Wrote pipeline spec documentation to ${file.absolutePath}")
   }
}


class DocumentationWriter {
   val objectMapper = jacksonObjectMapper()

   data class ParameterDocs(
      val parameter: String,
      val description: String,
      val required: Boolean = true,
      val defaultValue: String? = null
   )

   fun generateFor(classes: List<KClass<out PipelineTransportSpec>>): String {
      val documentationByDirection: Map<PipelineDirection, String> = classes.map { specClass ->
         val pipelineDocsAnnotation =
            specClass.findAnnotation<PipelineDocs>() ?: error("Class ${specClass.simpleName} is not documented")
         val title = "### ${pipelineDocsAnnotation.name}"

         val sample = pipelineDocsAnnotation.sample.objectInstance!!.sample

         val pipelineStatusDocs = """
| Pipeline Type Key | Direction | Maturity |
|-------------------|-----------|----------|
| `${sample.type}`    | `${sample.direction.name}` | ${
            pipelineDocsAnnotation.maturity.name.lowercase(Locale.getDefault())
               .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
         } |
         """.trim()

         val description = pipelineDocsAnnotation.docs

         val parameterDocs = specClass.primaryConstructor!!.parameters.mapNotNull { param ->
            val member = specClass.members.singleOrNull { it.name == param.name }
               ?: error("Param ${param.name} in class ${specClass.simpleName} does not have a backing field")
            val annotation = member.findAnnotation<PipelineParam>()
               ?: error("Param ${param.name} in class ${specClass.simpleName} is not documented")
            if (annotation.supressFromDocs) {
               return@mapNotNull null
            }
            ParameterDocs(param.name!!, annotation.description, !param.isOptional)
         }

         val parameters = """#### Parameters

The following configuration parameters are available:

|Parameter|Description|Required|Default Value|
|---------|-----------|--------|-------------|
${
            parameterDocs.joinToString("\n") { docs ->
               """
|`${docs.parameter}`|${docs.description}|${docs.required}|${docs.defaultValue.orElse("")}|
""".trim()
            }
         }
         """
         val wrappedSample = mapOf(
            sample.direction.name.lowercase(Locale.getDefault()) to sample
         )

         val example = """#### Example
            |
            |```json
            |${
            objectMapper.writerWithDefaultPrettyPrinter()
               .writeValueAsString(wrappedSample)
         }
            |```
         """.trimMargin()

         val docs = """$title
$pipelineStatusDocs

${description.trim()}

$parameters

$example
         """.trim()

         sample.direction to docs
      }.groupBy { it.first }
         .mapValues { (_, value: List<Pair<PipelineDirection, String>>) -> value.map { it.second } }
         .mapValues { (_, value: List<String>) -> value.joinToString("\n\n") }

      val inputDocs = documentationByDirection[PipelineDirection.INPUT] ?: ""
      val outputDocs = documentationByDirection[PipelineDirection.OUTPUT] ?: ""

      return """## Inputs

$inputDocs

## Outputs

$outputDocs
""".trim()

   }
}

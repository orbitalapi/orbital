package io.vyne.pipelines.jet.pipelines

import com.google.common.io.Resources
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.vyne.PackageIdentifier
import io.vyne.config.FileConfigSourceLoader
import io.vyne.pipelines.jet.api.transport.GenericPipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.file.FileWatcherStreamSourceSpec
import io.vyne.spring.config.EnvVariablesConfig
import io.vyne.utils.asA
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.io.path.exists

class PipelineConfigRepositoryTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Test
   fun `can load multiple configs from disk and resolve env variables`() {
      createProject()
      val configFile = folder.root.resolve("env.conf")
      configFile.writeText(
         """files {
         | directory : "/home/test"
         |}
      """.trimMargin()
      )
      val packageIdentifier = PackageIdentifier.fromId("com.orbitalhq/test/1.0.0")
      val repository = PipelineConfigRepository(
         listOf(
            FileConfigSourceLoader(
               folder.root.resolve("env.conf").toPath(),
               packageIdentifier = EnvVariablesConfig.PACKAGE_IDENTIFIER
            ),
            FileConfigSourceLoader(
               folder.root.resolve("pipelines").toPath(),
               packageIdentifier = packageIdentifier,
               glob = "*.conf"
            )
         )
      )

      //First, write a spec that contains an env variable
      val pipelineSpec = PipelineSpec(
         "test-pipeline",
         id = "pipeline-1",
         input = FileWatcherStreamSourceSpec(
            path = "\${files.directory}",
            typeName = "Foo"
         ),
         outputs = listOf(
            GenericPipelineTransportSpec(
               type = "output-1",
               direction = PipelineDirection.OUTPUT
            )
         )
      )
      repository.save(packageIdentifier, pipelineSpec)

      // Now read it back.
      val loadedPipelines = repository.loadPipelines()
      loadedPipelines.shouldHaveSize(1)
      val loadedPipeline =
         loadedPipelines.single().asA<PipelineSpec<FileWatcherStreamSourceSpec, GenericPipelineTransportSpec>>()
      // The path should've been resolved against the env variable also loaded.
      loadedPipeline.input.path.shouldBe("/home/test")
   }

   @Test
   fun `can write configs to disk`() {
      createProject()

      val packageIdentifier = PackageIdentifier.fromId("com.orbitalhq/test/1.0.0")
      val pipelinesPath = folder.root.resolve("pipelines").toPath()
      val repository = PipelineConfigRepository(
         listOf(
            FileConfigSourceLoader(
               pipelinesPath,
               packageIdentifier = packageIdentifier,
               glob = "*.conf"
            )
         )
      )
      val spec =
         PipelineSpec(
            "test-pipeline",
            id = "pipeline-1",
            input = GenericPipelineTransportSpec(
               type = "input-1",
               direction = PipelineDirection.INPUT
            ),
            outputs = listOf(
               GenericPipelineTransportSpec(
                  type = "output-1",
                  direction = PipelineDirection.OUTPUT
               )
            )
         )

      repository.save(packageIdentifier, spec)
      pipelinesPath.resolve("pipeline-1.conf").exists().shouldBeTrue()
   }

   fun createProject() {
      Resources.copy(
         Resources.getResource("sample-pipeline-project.conf"), folder.newFile("taxi.conf").outputStream()
      )

      folder.newFolder("pipelines")

      folder.root.toPath().resolve("orbital/config/env.conf").toFile().mkdirs()
      folder.root.toPath().resolve("pipelines").toFile().mkdirs()
   }
}

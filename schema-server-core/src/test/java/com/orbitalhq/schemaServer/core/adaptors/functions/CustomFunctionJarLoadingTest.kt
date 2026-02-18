package com.orbitalhq.schemaServer.core.adaptors.functions

import com.orbitalhq.SourcePackage
import com.orbitalhq.schemaServer.core.adaptors.taxi.TaxiSchemaSourcesAdaptor
import com.orbitalhq.schemaServer.core.adaptors.taxi.loader
import com.orbitalhq.schemaServer.core.file.deployProject
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.paths.shouldExist
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.taxilang.packagemanager.DependencyFetcherProvider
import org.taxilang.packagemanager.NoOpDependencyFetcherProvider
import java.io.File
import java.nio.file.Files
import java.nio.file.Path


// This test focuses on loading custom functions from jars, but not invoking them.
// Those tests are in the function-binding project
class CustomFunctionJarLoadingTest {
   @field:TempDir
   lateinit var projectTestDir: File

   @Test
   fun `can load a custom function from a jar file`() {
      projectTestDir.deployProject("custom-functions/function-in-jar-file")
      copyJar("../function-loading/test-java-project/target/test-java-project.jar", projectTestDir.resolve("functions/"))
      val sourcePackage = loadSourcePackage(projectTestDir.toPath())
      val schema = TaxiSchema.from(sourcePackage)

      // Verify the invoker is registered
      schema.functionRegistry.containsFunction(
         "com.orbitalhq.example.sayHello".fqn()
      ).shouldBeTrue()

      // Verify the function definition is loaded
      schema.taxi.function("com.orbitalhq.example.sayHello")
         .shouldNotBeNull()
   }


   private fun copyJar(jarPath: String, projectTestDir: File) {
      val jarLocation = Path.of(jarPath)
      withClue("Test java project jar file should exist") {
         jarLocation.toFile().shouldExist()
      }
      val destination = projectTestDir.toPath().resolve(jarLocation.fileName)
      Files.copy(jarLocation, destination )
      destination.shouldExist()
   }

}

fun loadSourcePackage(path: Path, dependencyFetcherProvider: DependencyFetcherProvider = NoOpDependencyFetcherProvider): SourcePackage {
   val loader = loader(path)
   val converter = TaxiSchemaSourcesAdaptor(dependencyFetcherProvider = dependencyFetcherProvider)
   val metadata = converter.buildMetadata(loader)
      .block()!!
   val source = converter.convert(metadata, loader).block()!!
   return source
}

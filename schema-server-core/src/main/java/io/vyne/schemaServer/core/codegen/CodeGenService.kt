package io.vyne.schemaServer.core.codegen

import io.vyne.schema.consumer.SchemaStore
import io.vyne.schemaServer.codegen.CodeGenApi
import lang.taxi.generators.TaxiProjectEnvironment
import lang.taxi.generators.typescript.TypeScriptGenerator
import lang.taxi.packages.TaxiPackageProject
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.nio.file.Path

@RestController
class CodeGenService(
   private val schemaStore: SchemaStore
) : CodeGenApi {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   @GetMapping("/api/taxonomy/typescript")
   override fun getTypeScriptTaxonomy(): Mono<String> {
      return Mono.create { sink ->
         val taxonomy = TypeScriptGenerator().generate(schemaStore.schemaSet.schema.taxi, emptyList(), MockEnvironment)

         when (val content = taxonomy.firstOrNull()?.content) {
            null -> sink.error(IllegalStateException("Failed to generate the Typescript taxonomy"))
            else -> sink.success(content)
         }
      }.subscribeOn(Schedulers.boundedElastic())
   }
}

/**
 * Not actually needed in this context
 */
private object MockEnvironment : TaxiProjectEnvironment {
   override val projectRoot: Path
      get() = TODO("Not yet implemented")
   override val outputPath: Path
      get() = TODO("Not yet implemented")
   override val project: TaxiPackageProject
      get() = TODO("Not yet implemented")
}



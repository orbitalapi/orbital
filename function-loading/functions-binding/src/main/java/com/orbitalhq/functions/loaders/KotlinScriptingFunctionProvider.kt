package com.orbitalhq.functions.loaders

import com.orbitalhq.functions.TaxiFunctionProvider
import com.orbitalhq.VersionedSource
import com.orbitalhq.PackageMetadata
import com.orbitalhq.functions.kotlin.KotlinTaxiFunctionScript
import com.orbitalhq.functions.kotlin.TaxiFunctionCompilationConfiguration
import mu.KotlinLogging
import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

class KotlinScriptFunctionProvider : CustomFunctionProvider {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val scriptingHost = BasicJvmScriptingHost()

   override fun loadFunctionClasses(
      sourceFiles: List<VersionedSource>,
      packageMetadata: PackageMetadata
   ): List<TaxiFunctionProvider> {

      val providers = mutableListOf<TaxiFunctionProvider>()

      sourceFiles.forEach { sourceFile ->
         val path = sourceFile.path
         if (path == null) {
            logger.warn { "Can't load Kotlin script for ${sourceFile.packageQualifiedName} as no path was provided" }
            return@forEach
         }

         val file = File(path)
         if (!file.exists()) {
            logger.warn { "Kotlin script file not found: $path" }
            return@forEach
         }

         logger.info { "Compiling Kotlin script: ${file.absolutePath}" }

         val compilationConfiguration =
            createJvmCompilationConfigurationFromTemplate<KotlinTaxiFunctionScript>()
         val evalResult: ResultWithDiagnostics<EvaluationResult> = scriptingHost.eval(
            file.toScriptSource(),
            compilationConfiguration,
            null
         )
         val returnValue = try {
            evalResult.valueOrThrow().returnValue
         } catch (e: Exception) {
            logger.error(e) { "Failed to evaluate script $path" }
            return@forEach
         }

         // Return value may be the provider itself, or a script instance exposing providers
         val provider = when (returnValue) {
            is TaxiFunctionProvider -> returnValue
            is kotlin.script.experimental.api.ResultValue.Value -> {
               (returnValue.value as? TaxiFunctionProvider)
            }
            is kotlin.script.experimental.api.ResultValue.Unit -> {
               (returnValue.scriptInstance as? TaxiFunctionProvider)
            }

            else -> null
         }

         if (provider != null) {
            logger.info { "Registering provider from script ${file.name}" }
            providers.add(provider)
         } else {
            logger.warn { "Script ${file.name} did not evaluate to a TaxiFunctionProvider" }
         }
      }

      return providers
   }
}

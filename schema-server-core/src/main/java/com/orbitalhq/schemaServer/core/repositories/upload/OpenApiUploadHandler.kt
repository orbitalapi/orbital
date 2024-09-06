package com.orbitalhq.schemaServer.core.repositories.upload

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemaServer.packages.OpenApiPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.PackageType
import com.orbitalhq.schemaServer.repositories.AddFileProjectRequest
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText

@Component
class OpenApiProjectUploadHandler : ProjectUploadHandler {
   override val packageType: PackageType = PackageType.OpenApi

   override fun processUpload(
      packageId: PackageIdentifier,
      requestParams: Map<String, List<String>>,
      payload: ByteArray,
      projectRoot: Path
   ): AddFileProjectRequest {

      val openApiSpec = payload.toString(Charsets.UTF_8)
      val format = detectFormat(openApiSpec)
      val targetFile = projectRoot.resolve("spec.${format.suffix}")
      logger.info { "Will save OpenAPI spec for $packageId to ${targetFile.absolutePathString()}" }
      targetFile.writeText(openApiSpec)
      val defaultNamespace = requestParams.singleOrBadRequest(OpenApiPackageLoaderSpec::defaultNamespace)
      val baseUrl = requestParams.singleOrNullOrBadRequest(OpenApiPackageLoaderSpec::serviceBasePath)

      return AddFileProjectRequest(
         targetFile.absolutePathString(),
         false,
         OpenApiPackageLoaderSpec(
            packageId,
            defaultNamespace = defaultNamespace,
            serviceBasePath = baseUrl
         )
      )
   }

   companion object {
      private val logger = KotlinLogging.logger {}
      fun detectFormat(content: String):OpenApiFormat {
         return if (content.trim().startsWith("{")) {
            OpenApiFormat.JSON
         } else {
            OpenApiFormat.YAML
         }
      }
      enum class OpenApiFormat(val suffix: String) {
         JSON("oas.json"),
         YAML("oas.yaml")
      }
   }
}

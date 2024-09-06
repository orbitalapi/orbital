package com.orbitalhq.schemaServer.core.repositories.upload

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemaServer.packages.AvroPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.PackageType
import com.orbitalhq.schemaServer.repositories.AddFileProjectRequest
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText

@Component
class AvroProjectUploadHandler : ProjectUploadHandler {
   override val packageType: PackageType = PackageType.Avro

   override fun processUpload(
      packageId: PackageIdentifier,
      requestParams: Map<String, List<String>>,
      payload: ByteArray,
      projectRoot: Path
   ): AddFileProjectRequest {
      val avroSpec = payload.toString(Charsets.UTF_8)
      val targetFile = projectRoot.resolve("spec.avsc")
      logger.info { "Will save Avro spec for $packageId to ${targetFile.absolutePathString()}" }
      targetFile.writeText(avroSpec)

      return AddFileProjectRequest(
         targetFile.absolutePathString(),
         false,
         AvroPackageLoaderSpec(
            packageId,
         )
      )
   }

   companion object {
      private val logger = KotlinLogging.logger {}
   }
}

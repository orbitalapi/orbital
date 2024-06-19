package com.orbitalhq.cockpit.core.schemas.importing.protobuf

import com.orbitalhq.cockpit.core.schemas.importing.*
import com.orbitalhq.spring.http.BadRequestException
import lang.taxi.generators.protobuf.ProtobufUtils
import lang.taxi.generators.protobuf.TaxiGenerator
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.fakefilesystem.FakeFileSystem
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.URI
import java.nio.file.Paths
import kotlin.reflect.KClass

@Component
class ProtobufSchemaConverter(
   webClient: WebClient = WebClient.create(),
) : SchemaConverter<ProtobufSchemaConverterOptions>, BaseUrlLoadingSchemaConverter(webClient) {
   override val supportedFormats: List<String> = listOf(PROTOBUF_FORMAT)
   override val conversionParamsType: KClass<ProtobufSchemaConverterOptions> = ProtobufSchemaConverterOptions::class

   companion object {
      const val PROTOBUF_FORMAT = "protobuf"
   }

   override fun convert(
      request: SchemaConversionRequest,
      options: ProtobufSchemaConverterOptions
   ): Mono<SourcePackageWithMessages> {
      val fileSystem = if (options.url != null) {
         loadFromUrl(options)
      } else {
         loadFromProvidedProtobuf(options)
      }

      return fileSystem.map { fs ->
         TaxiGenerator(fs)
            .addSchemaRoot("/")
            .generate()
      }.map { taxiCode ->
         taxiCode.toSourcePackageWithMessages(request.packageIdentifier, generatedImportedFileName("Protobuf"))
      }
   }

   private fun loadFromProvidedProtobuf(options: ProtobufSchemaConverterOptions): Mono<FileSystem> {
      val protobuf = options.protobuf ?: throw BadRequestException("protobuf must be supplied")
      val filename = options.filename ?: throw BadRequestException("filename must be supplied")
      val packageName = ProtobufUtils.findPackageName(protobuf)
      return loadFromFakeFilesystem(protobuf, filename, packageName)
   }

   private fun loadFromFakeFilesystem(protobuf: String, filename: String, packageName: String): Mono<FileSystem> {
      return Mono.fromCallable {
         val filePath = Paths.get(packageName.replace(".", "/"), filename)

         // Protobuf needs files to process, as the file location
         // is important.
         // So, we create an in-memory file system.
         // In the future, this may cause issues, if we get really big protobufs.
         // For now, let's go with it.
         val fs = FakeFileSystem()
         fs.createDirectories(filePath.parent.toOkioPath())
         fs.write(filePath.toOkioPath(), true) {
            writeUtf8(protobuf)
         }
         fs
      }

   }

   private fun loadFromUrl(options: ProtobufSchemaConverterOptions): Mono<FileSystem> {
      return loadSchema(options.url!!)
         .flatMap { schema ->
            val uri = URI.create(options.url)
            val packageName = ProtobufUtils.findPackageName(schema)

            val fileName = uri.toURL().file.let {
               if (!it.endsWith(".proto")) {
                  "$it.proto"
               } else {
                  it
               }
            }
            loadFromFakeFilesystem(schema,fileName, packageName)
         }
   }
}


data class ProtobufSchemaConverterOptions(
   val protobuf: String? = null,
   val filename: String? = null,
   val url: String? = null,
)

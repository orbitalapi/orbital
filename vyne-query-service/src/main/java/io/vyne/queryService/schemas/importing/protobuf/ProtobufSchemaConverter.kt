package io.vyne.queryService.schemas.importing.protobuf

import io.vyne.queryService.schemas.importing.BaseUrlLoadingSchemaConverter
import io.vyne.queryService.schemas.importing.SchemaConversionRequest
import io.vyne.queryService.schemas.importing.SchemaConverter
import lang.taxi.generators.GeneratedTaxiCode
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
   ): Mono<GeneratedTaxiCode> {
      val fileSystem = if (options.url != null) {
         loadFromUrl(options)
      } else {
         TODO("Not yet implemented")
      }

      return fileSystem.map { fs ->
         TaxiGenerator(fs)
            .addSchemaRoot("/")
            .generate()
      }


   }

   private fun loadFromUrl(options: ProtobufSchemaConverterOptions): Mono<FileSystem> {
      return loadSchema(options.url!!)
         .map { schema ->
            val uri = URI.create(options.url)
            val packageName = ProtobufUtils.findPackageName(schema)

            val fileName = uri.toURL().file.let {
               if (!it.endsWith(".proto")) {
                  "$it.proto"
               } else {
                  it
               }
            }
            val filePath = Paths.get(packageName.replace(".","/") , fileName)

            // Protobuf needs files to process, as the file location
            // is important.
            // So, we create an in-memory file system.
            // In the future, this may cause issues, if we get really big protobufs.
            // For now, let's go with it.
            val fs = FakeFileSystem()
            fs.createDirectories(filePath.parent.toOkioPath())
            fs.write(filePath.toOkioPath(), true) {
               writeUtf8(schema)
            }
            fs
         }
   }
}


data class ProtobufSchemaConverterOptions(
   val protobuf: String? = null,
   val url: String? = null,
)

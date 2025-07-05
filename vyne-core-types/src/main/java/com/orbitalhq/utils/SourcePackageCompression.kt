package com.orbitalhq.utils

import com.fasterxml.jackson.module.kotlin.readValue
import com.orbitalhq.DefaultPackageMetadata
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.models.json.Jackson
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import mu.KotlinLogging
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.time.measureTimedValue

@OptIn(InternalSerializationApi::class)
object SourcePackageCompression {
   private val module: SerializersModule = SerializersModule {
      polymorphic(PackageMetadata::class, DefaultPackageMetadata::class, DefaultPackageMetadata::class.serializer())
   }
   private val logger = KotlinLogging.logger {}
   val cbor = Cbor { serializersModule = module }
   val json = Json { serializersModule = module }

   // We use Jackson for serializing polymorphic types (like Map<String,Any>, as
   // used for params.  Kotlin serialziation doesn't like working with types there isn't
   // a predefined schema for.
   private val jackson = Jackson.newObjectMapperWithDefaults()

   fun compressSourcePackages(packages: List<SourcePackage>): ByteArray {
      val byteArrayOutputStream = ByteArrayOutputStream()
      val gzip = GZIPOutputStream(byteArrayOutputStream)
      json.encodeToStream(packages, gzip)
      gzip.close()
      return byteArrayOutputStream.toByteArray()
   }

   fun decompressSourcePackages(byteArray: ByteArray): List<SourcePackage> {
      val timedResult = measureTimedValue {
         val gzip = GZIPInputStream(ByteArrayInputStream(byteArray))
         json.decodeFromStream<List<SourcePackage>>(gzip)
      }
      logger.info { "Decoding SourcePackages took ${timedResult.duration}" }
      return timedResult.value
   }

   fun compressArgs(parameters: Map<String, Any?>): ByteArray {
      val byteArrayOutputStream = ByteArrayOutputStream()
      val gzip = GZIPOutputStream(byteArrayOutputStream)
      jackson.writeValue(gzip, parameters)
      gzip.close()
      return byteArrayOutputStream.toByteArray()
   }

   fun decompressArgs(byteArray: ByteArray): Map<String, Any?> {
      val gzip = GZIPInputStream(ByteArrayInputStream(byteArray))
      return jackson.readValue<Map<String, Any>>(gzip)
   }
}

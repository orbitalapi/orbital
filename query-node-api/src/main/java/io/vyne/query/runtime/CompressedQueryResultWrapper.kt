package io.vyne.query.runtime

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.SourcePackage
import io.vyne.models.json.Jackson
import io.vyne.utils.formatAsFileSize
import kotlinx.serialization.json.decodeFromStream
import mu.KotlinLogging
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

/**
 * A wrapper that returns results from a native query function,
 * compressed as much as possible
 *
 * Because gzip compression isn't enabled by default on the
 * Lambda responses, (and because lambdas require us to write responses
 * as JSON), we have a simple wrapper which exposes a Gzipped byteArray.
 *
 * Also, because we're focussing on compression here, nulls are omitted.
 */
@OptIn(ExperimentalTime::class)
data class CompressedQueryResultWrapper(val r:ByteArray) {
   companion object {
      val mapper = Jackson.newObjectMapperWithDefaults()
         .setSerializationInclusion(JsonInclude.Include.NON_NULL)

      private val logger = KotlinLogging.logger {}
      fun forResult(result:Any): CompressedQueryResultWrapper {
         val byteArrayOutputStream = ByteArrayOutputStream()
         val gzip = GZIPOutputStream(byteArrayOutputStream)
         mapper.writeValue(gzip, result)
         gzip.close()
         val byteArray = byteArrayOutputStream.toByteArray()
         logger.info { "Message response compressed to approx. ${byteArray.size.formatAsFileSize}" }
         return CompressedQueryResultWrapper(byteArray)
      }
   }

   fun decompress():Any {
      val timedResult = measureTimedValue {
         val gzip = GZIPInputStream(ByteArrayInputStream(r))
         mapper.readValue<Any>(gzip)
      }
      logger.info { "Decompressing result took ${timedResult.duration}" }
      return timedResult.value
   }

}

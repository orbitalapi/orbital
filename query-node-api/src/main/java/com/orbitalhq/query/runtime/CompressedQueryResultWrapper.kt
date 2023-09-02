package com.orbitalhq.query.runtime

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.readValue
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.QueryFailedException
import com.orbitalhq.utils.formatAsFileSize
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
data class CompressedQueryResultWrapper(val r: ByteArray, val error: String? = null) {
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

      fun forError(error: String): CompressedQueryResultWrapper {
         return CompressedQueryResultWrapper(
            ByteArray(0),
            error
         )
      }
   }

   /**
    * Returns the result decompressed, or throws a QueryFailedException if an error
    * was returned
    */
   fun decompressOrThrow(): Any {
      if (error != null) {
         throw QueryFailedException(error)
      } else {
         return decompress()
      }
   }
   fun decompress(): Any {
      val timedResult = measureTimedValue {
         val gzip = GZIPInputStream(ByteArrayInputStream(r))
         mapper.readValue<Any>(gzip)
      }
      logger.info { "Decompressing result took ${timedResult.duration}" }
      return timedResult.value
   }

   fun decompressJson(): String {
      val timedResult = measureTimedValue {
         val gzip: GZIPInputStream = GZIPInputStream(ByteArrayInputStream(r))
         String(gzip.readAllBytes())
      }
      logger.info { "Decompressing result took ${timedResult.duration}" }
      return timedResult.value
   }

}

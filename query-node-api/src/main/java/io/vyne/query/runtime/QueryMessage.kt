package io.vyne.query.runtime

import io.vyne.DefaultPackageMetadata
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.auth.tokens.AuthConfig
import io.vyne.connectors.config.ConnectorsConfig
import io.vyne.http.ServicesConfig
import io.vyne.query.ResultMode
import kotlinx.serialization.*
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.modules.SerializersModule
import mu.KotlinLogging
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue


/**
 * Fully encapsulates everything required to
 * execute a query on a standalone Vyne instance.
 *
 * This message is intended to be sent across the wire to query nodes (such as Lambdas).
 * Therefore, it's designed to be lightweight on the wire.
 *
 * Specifically, the sources are sent as a zipped ByteArray of a JSON encoded string.
 *
 * In testing with large schemas, we found this dramatically dropped the size of the payload (from 5.8MB to 500k)
 */
@Serializable
@OptIn(ExperimentalTime::class, ExperimentalSerializationApi::class)
data class QueryMessage(
   val query: String,
   val sourcePackageZip: ByteArray,
   // Careful: If you use an object mapper to convert this to another connection type
   // (eg:ObjectMapper.convertValue<JdbcConnections>(connections)),
   // you need to register that type in NativeQueryNodeRuntimeHints
   // as it's not available for reflection at runtime in this image.
   val connections: ConnectorsConfig,
   val authTokens: AuthConfig,
   val services: ServicesConfig,
   val resultMode: ResultMode = ResultMode.RAW,
   val mediaType: String,
   val clientQueryId: String,
   ) {
   constructor(
      query: String,
      sourcePackages: List<SourcePackage>,
      // Careful: If you use an object mapper to convert this to another connection type
      // (eg:ObjectMapper.convertValue<JdbcConnections>(connections)),
      // you need to register that type in NativeQueryNodeRuntimeHints
      // as it's not available for reflection at runtime in this image.
      connections: ConnectorsConfig,
      authTokens: AuthConfig,
      services: ServicesConfig,
      resultMode: ResultMode = ResultMode.RAW,
      mediaType: String,
      clientQueryId: String,
   ) : this(
      query,
      compressSourcePackages(sourcePackages),
      connections, authTokens, services, resultMode, mediaType, clientQueryId
   )

   companion object {
      @OptIn(InternalSerializationApi::class)
      private val module: SerializersModule = SerializersModule {
         polymorphic(PackageMetadata::class, DefaultPackageMetadata::class, DefaultPackageMetadata::class.serializer())
      }
      private val logger = KotlinLogging.logger {}
      val cbor = Cbor { serializersModule = module }
      val json = Json { serializersModule = module }

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
   }

   fun sourcePackages():List<SourcePackage> = decompressSourcePackages(this.sourcePackageZip)

   // Have to implement equals & hashcode ourselves, b/c of the ByteArray field
   override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as QueryMessage

      if (query != other.query) return false
      if (!sourcePackageZip.contentEquals(other.sourcePackageZip)) return false
      if (connections != other.connections) return false
      if (authTokens != other.authTokens) return false
      if (services != other.services) return false
      if (resultMode != other.resultMode) return false
      if (mediaType != other.mediaType) return false
      return clientQueryId == other.clientQueryId
   }

   // Have to implement equals & hashcode ourselves, b/c of the ByteArray field
   override fun hashCode(): Int {
      var result = query.hashCode()
      result = 31 * result + sourcePackageZip.contentHashCode()
      result = 31 * result + connections.hashCode()
      result = 31 * result + authTokens.hashCode()
      result = 31 * result + services.hashCode()
      result = 31 * result + resultMode.hashCode()
      result = 31 * result + mediaType.hashCode()
      result = 31 * result + clientQueryId.hashCode()
      return result
   }
}

/**
 * A very small wrapper around a QueryMessage, which has been encoded as a cbor ByteArray
 *
 * This is because AWS Lambda expects to receive a JSON message, not a binary one.
 * Suspect there is a way to remove this hacky workaround (and just have the Labmda accept the
 * CBOR directly), but I can't work it our right now.
 */
@OptIn(ExperimentalSerializationApi::class, ExperimentalTime::class)
data class QueryMessageCborWrapper(val m:ByteArray) {
   companion object {
      private val logger = KotlinLogging.logger {}
      fun from(queryMessage: QueryMessage): QueryMessageCborWrapper {
         val messageBytes = QueryMessage.cbor.encodeToByteArray(queryMessage)
         return QueryMessageCborWrapper(messageBytes)
      }
   }
   fun message():QueryMessage {
      val queryMessage = measureTimedValue {
         QueryMessage.cbor.decodeFromByteArray<QueryMessage>(m)
      }
      logger.info { "Deserializing query message took ${queryMessage.duration}" }
      return queryMessage.value
   }
   fun size() = m.size
}

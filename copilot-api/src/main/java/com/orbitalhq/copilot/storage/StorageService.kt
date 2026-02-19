package com.orbitalhq.copilot.storage

import com.orbitalhq.copilot.BadRequestException
import org.springframework.core.io.Resource
import org.springframework.http.MediaTypeFactory
import reactor.core.publisher.Mono
import java.time.Instant
import kotlin.jvm.optionals.getOrElse

/**
 * A service which allows for uploaded files from a chat session.
 * Both Orbital and Copilot must be configured to use the same service (local disk / S3 etc)
 *
 * Designed around the s3 concept of presigned urls, but we provide
 * a local implementation for dev.
 *
 * The idea is that when uploading, rather than the client sending direct to our
 * backend, it's more secure to provide a single-use tightly controlled upload url.
 *
 * So the client requests an upload url token - this service issues one (by interacting with
 * s3 if required), and the client then uploads to the url directly.
 *
 */
// Core interface
interface StorageService {
   fun retrieve(key: String): Mono<Resource>
   fun getPresignedUploadUrl(sessionId: String, filename: String, expirationMinutes: Int = 5): Mono<PresignedUploadUrl>
   fun delete(key: String): Mono<Void>
   fun deleteBySessionId(sessionId: String): Mono<Void>
}

data class StoredFileReference(
   val key: String,
   val filename: String,
   val size: Long,
   val contentType: String?
)

data class PresignedUploadUrl(
   val uploadUrl: String,
   val filename: String,
   val key: String,
   val expiresAt: Instant
)

fun inferContentType(filename: String): String {
   return  MediaTypeFactory.getMediaType(filename).map { it.toString() }
      .getOrElse { "application/octet-stream" }

}

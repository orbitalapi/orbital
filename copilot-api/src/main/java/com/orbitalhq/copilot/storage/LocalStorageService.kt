package com.orbitalhq.copilot.storage

import com.orbitalhq.copilot.BadRequestException
import mu.KotlinLogging
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.name


// Local Filesystem Implementation
class LocalStorageService(
   private val root: Path
) : StorageService {
   private val uploadTokens = ConcurrentHashMap<String, UploadToken>()
   init {
      if (!Files.exists(root)) {
         Files.createDirectories(root)
      }
      logger.info { "Copilot is using a local filestore at ${root.absolute().absolutePathString()}" }
   }

   fun store(
      token: String,
      content: Flux<DataBuffer>
   ): Mono<StoredFileReference> {

      val uploadToken = uploadTokens[token]
         ?: return Mono.error(BadRequestException("Invalid or expired upload token"))

      uploadTokens.remove(token)
      if (Instant.now().isAfter(uploadToken.expiresAt)) {
         return Mono.error(BadRequestException("Upload token expired"))
      }

      Files.createDirectories(uploadToken.path.parent)


      return DataBufferUtils.write(content, uploadToken.path, StandardOpenOption.CREATE)
         .then(Mono.fromCallable {
            StoredFileReference(
               key = token,
               filename = uploadToken.path.name,
               size = Files.size(uploadToken.path),
               contentType = inferContentType(uploadToken.path.name)
            )
         })
   }

   override fun retrieve(key: String): Mono<Resource> {
      return Mono.fromCallable {
         val filePath = root.resolve(key)
         if (!Files.exists(filePath)) {
            throw FileNotFoundException("File not found: $key")
         }
         FileSystemResource(filePath) as Resource
      }.subscribeOn(Schedulers.boundedElastic())
   }

   override fun getPresignedUploadUrl(
      sessionId: String,
      filename: String,
      expirationMinutes: Int
   ): Mono<PresignedUploadUrl> {
      // For local storage, we can't do presigned URLs
      // Instead, return a token-based upload endpoint
      val key = generateKey(sessionId, filename)
      val token = UUID.randomUUID().toString()
      val expiresAt = Instant.now().plusSeconds(expirationMinutes * 60L)

      // Store the token mapping (in-memory is fine for local dev)
      uploadTokens[token] = UploadToken(key, root.resolve(key).absolute(), expiresAt)

      return Mono.just(
         PresignedUploadUrl(
            filename = filename,
            uploadUrl = "/api/copilot/storage/upload/$token",
            key = key,
            expiresAt = expiresAt
         )
      )
   }

   override fun delete(key: String): Mono<Void> {
      return Mono.fromRunnable<Void> {
         val filePath = root.resolve(key)
         if (Files.exists(filePath)) {
            Files.delete(filePath)
         }
      }.subscribeOn(Schedulers.boundedElastic())
         .then()
   }

   override fun deleteBySessionId(sessionId: String): Mono<Void> {
      return Mono.fromRunnable<Void> {
         val sessionDir = root.resolve(sessionId)
         if (Files.exists(sessionDir)) {
            Files.walk(sessionDir)
               .sorted(Comparator.reverseOrder())
               .forEach { Files.delete(it) }
         }
      }.subscribeOn(Schedulers.boundedElastic())
         .then()
   }

   private fun generateKey(sessionId: String, filename: String): String {
      val uuid = UUID.randomUUID()
      val sanitizedFilename = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
      return "$sessionId/$uuid-$sanitizedFilename"
   }

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   data class UploadToken(
      val key: String,
      val path: Path,
      val expiresAt: Instant
   )


}

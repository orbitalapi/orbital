package org.taxilang.playground.sharing

import com.google.common.hash.Hashing
import io.vyne.utils.Ids
import mu.KotlinLogging
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*
import javax.persistence.*

@RestController
class SchemaShareService(private val repository: StoredSchemaRepository) {

   private val logger = KotlinLogging.logger {}

   @GetMapping("/api/schema/share/{slug}")
   fun getStoredSchema(@PathVariable("slug") slug: String): ResponseEntity<String> {
      logger.info { "Loading schema for slug $slug" }
      try {
         val schema = repository.findByUriSlug(slug)
         return ResponseEntity.ok(schema.taxi)
      } catch (e: EntityNotFoundException) {
         throw NotFoundException("No schema found at path $slug")
      }
   }

   @PostMapping("/api/schema/share")
   fun getShareableLink(@RequestBody taxi: String): SharedSchemaResponse {
      validate(taxi.trim().isNotEmpty(), "You gots to provide at least something here.")
      validate(taxi.length < StoredSchema.MAX_SIZE, "That's too much, man!")

      logger.info { "Attempting to save schema" }
      val sha = Hashing.sha256().hashBytes(taxi.trim().toByteArray()).toString()
      val storedSchema = repository.findByContentSha(sha)
         ?: repository.save(
            StoredSchema(
               id = UUID.randomUUID().toString(),
               uriSlug = Ids.id("", 10),
               taxi = taxi,
               createdDate = Instant.now(),
               contentSha = sha
            )
         )
      logger.info { "Schema created with slug ${storedSchema.uriSlug} and id ${storedSchema.id}" }

      return SharedSchemaResponse(
         "/s/${storedSchema.uriSlug}", storedSchema.uriSlug
      )
   }

   fun validate(value: Boolean, message: String) {
      if (!value) {
         throw BadRequestException(message)
      }
   }
}


data class SharedSchemaResponse(
   val uri: String, val id: String
)

@Entity
data class StoredSchema(
   @Id val id: String,
   @Column(unique = true)
   val uriSlug: String,
   @Column(length = MAX_SIZE)
   val taxi: String,
   @Column(unique = true)
   val contentSha: String,
   val createdDate: Instant
) {
   companion object {
      const val MAX_SIZE: Int = 500_000;
   }
}

interface StoredSchemaRepository : JpaRepository<StoredSchema, String> {
   fun findByUriSlug(slug: String): StoredSchema
   fun findByContentSha(sha: String): StoredSchema?
}


@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException(message: String) : RuntimeException(message)

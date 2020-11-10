package io.vyne.cask.ingest

import io.vyne.schemas.VersionedType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.PagingAndSortingRepository
import java.time.Instant
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class IngestionError(
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   val id: Long = 0,
   @Column(name = "error")
   val error: String,
   @Column(name = "cask_message_id")
   val caskMessageId: String,
   @Column(name = "type_fqn")
   val fullyQualifiedName: String,
   @Column(name = "inserted_at")
   val insertedAt: Instant = Instant.now()
) {
   companion object {
      fun fromThrowable(t: Throwable, messageId: String, versionedType: VersionedType) = IngestionError(
         caskMessageId = messageId,
         error = t.message ?: "Unknown error",
         fullyQualifiedName = versionedType.fullyQualifiedName)
   }
}

interface IngestionErrorRepository : PagingAndSortingRepository<IngestionError, String> {
   fun findByInsertedAtBetween(start: Instant, end: Instant, pageable: Pageable): Page<IngestionError>
   fun findByFullyQualifiedNameAndInsertedAtBetweenOrderByInsertedAtDesc(fullyQualifiedName: String, start: Instant, end: Instant, pageable: Pageable): Page<IngestionError>
   fun countByFullyQualifiedNameAndInsertedAtBetween(fullyQualifiedName: String, start: Instant, end: Instant): Int
}



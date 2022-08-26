package io.vyne.schemaServer.changelog

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.vyne.PackageIdentifier
import io.vyne.UnversionedPackageIdentifier
import io.vyne.schema.publisher.SchemaUpdatedMessage
import io.vyne.schemaServer.config.SchemaUpdateNotifier
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.QualifiedNameAsStringDeserializer
import io.vyne.schemas.QualifiedNameAsStringSerializer
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.Instant

@RestController
class ChangeLogService(
   private val updateNotifier: SchemaUpdateNotifier,
   private val diffFactory: ChangeLogDiffFactory = ChangeLogDiffFactory()
) : ChangelogApi {

   // TODO : This needs to be persisted
   private val changeLogEntries: MutableList<ChangeLogEntry> = mutableListOf()

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      updateNotifier.schemaUpdates.subscribe { message ->
         constructChangeLogEntry(message)
      }
   }

   @GetMapping("/api/schema/changelog")
   override fun getChangelog(): Mono<List<ChangeLogEntry>> {
      return Mono.just(changeLog.reversed())
   }

   private fun constructChangeLogEntry(message: SchemaUpdatedMessage) {

      val diffs = diffFactory.buildDiffs(message.oldSchema, message.schema)
      if (diffs.isEmpty()) {
         return
      }
      val affectedPackages = message.deltas.map { it.packageId }
         .distinct()
      val changeLogEntry = ChangeLogEntry(
         Instant.now(),
         affectedPackages,
         diffs
      )
      changeLogEntries.add(
         changeLogEntry
      )
      logger.debug { "Change log updated, new entry containing ${changeLogEntry.diffs.size} diffs" }
   }

   val changeLog: List<ChangeLogEntry>
      get() {
         return changeLogEntries.toList()
      }

}


package com.orbitalhq.schemaServer.core.changelog

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.UriSafePackageIdentifier
import com.orbitalhq.schema.publisher.SchemaUpdatedMessage
import com.orbitalhq.schemaServer.changelog.ChangeLogEntry
import com.orbitalhq.schemaServer.changelog.ChangelogApi
import com.orbitalhq.schemaServer.core.config.SchemaUpdateNotifier
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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

   @GetMapping("/api/changelog")
   override fun getChangelog(): Mono<List<ChangeLogEntry>> {
      return Mono.just(changeLog.reversed())
   }

   @GetMapping("/api/changelog/{packageName}")
   override fun getChangelog(@PathVariable("packageName") packageName: UriSafePackageIdentifier): Mono<List<ChangeLogEntry>> {
      val unversionedIdentifier = PackageIdentifier.uriSafeIdToUnversionedIdentifier(packageName)
      val entries = changeLog
         .filter { it.affectedPackages.any { affectedPackage -> affectedPackage == unversionedIdentifier } }
         .reversed()
      return Mono.just(entries)
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


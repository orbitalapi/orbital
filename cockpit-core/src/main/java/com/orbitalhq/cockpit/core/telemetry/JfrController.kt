package com.orbitalhq.cockpit.core.telemetry

import jdk.jfr.Configuration
import jdk.jfr.Recording
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.nio.file.Files

/**
 * This class allows starting, stopping and downloading of Jfr flight recorder
 * data directly from Orbital. This provides a convenient way of getting low-level diagnostic data from a customer.
 * HOWEVER ,this MUST be disabled by default, and require a flag to turn on.
 * There's risks in leaving this running, as it can quickly fill up disk.
 */
@ConditionalOnProperty("vyne.debugging.jfr.enabled", havingValue = "true", matchIfMissing = false)
@RestController
class JfrController {

   companion object {
      private val logger = KotlinLogging.logger {}
   }
   init {
      logger.info { "JFR Controller is enabled" }
   }
   @Volatile
   private var recording: Recording? = null

   @PostMapping("/api/debug/jfr/start")
   fun startRecording(
   ): String {
      if (recording != null) return "Recording already in progress"

      val config = Configuration.getConfiguration("profile")

      recording = Recording(config).apply {
         maxSize = 200_000_000 // 200MB
         maxAge = java.time.Duration.ofMinutes(30)
         start()
      }

      logger.warn { "======================================================" }
      logger.warn { "======  Starting JFR Flight Recording     ============" }
      logger.warn { "======================================================" }
      return "Recording started"
   }

   @PostMapping("/api/debug/jfr/stop")
   fun stopRecording(): String {
      val rec = recording ?: return "No active recording"

      rec.stop()

      logger.warn { "======================================================" }
      logger.warn { "======  Stopping JFR Flight Recording     ============" }
      logger.warn { "======================================================" }

      return "Recording stopped. Use /api/debug/jfr/download to fetch the file."
   }

   @GetMapping("/api/debug/jfr/download")
   fun download(): Mono<ResponseEntity<InputStreamResource>> {
      val rec = recording ?: return Mono.just(
         ResponseEntity.badRequest().build()
      )

      val filename = "recording-${System.currentTimeMillis()}.jfr"
      val tempPath = Files.createTempFile("jfr-", ".jfr")

      // Dump to disk (blocking, but short-lived)
      rec.dump(tempPath)
      rec.close()
      recording = null

      val resource = InputStreamResource(Files.newInputStream(tempPath))

      return Mono.just(
         ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=$filename")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource)
      ).doFinally {
         Files.deleteIfExists(tempPath)
      }
   }
}

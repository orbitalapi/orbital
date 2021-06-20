package io.vyne.cask

import io.vyne.utils.log
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class CaskEvictionScheduler(private val caskService: CaskService, private val clock: Clock = Clock.systemUTC()) {

    @Scheduled(cron = "\${cask.eviction.cron}")
    fun evict() {
        caskService.getCasks().forEach {
            try {
                log().info("Evicting cask entries for type ${it.qualifiedTypeName}/${it.versionHash}, table ${it.tableName}, daysToRetain ${it.daysToRetain}")
                val since = Instant.now(clock).minus(it.daysToRetain.toLong(), ChronoUnit.DAYS)
                caskService.evict(it.tableName, since)
            } catch (e: Exception) {
                log().error("Failed to evict cask entries", e)
            }

        }
    }
}
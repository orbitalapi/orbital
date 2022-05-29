package io.vyne.pipelines.jet.source

import org.springframework.scheduling.support.CronSequenceGenerator
import java.time.Instant
import java.util.*

internal fun CronSequenceGenerator.next(lastRunTime: Instant): Instant {
   return this.next(Date.from(lastRunTime)).toInstant()
}

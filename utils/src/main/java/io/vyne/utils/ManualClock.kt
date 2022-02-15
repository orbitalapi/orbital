package io.vyne.utils

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class ManualClock(private val startTime: Instant) : Clock() {
   private var time: Long = startTime.toEpochMilli()
   fun advanceMillis(millis: Long) {
      this.time += millis
   }

   override fun withZone(zone: ZoneId?): Clock {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun getZone(): ZoneId {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun instant(): Instant {
      return Instant.ofEpochMilli(time)
   }

}

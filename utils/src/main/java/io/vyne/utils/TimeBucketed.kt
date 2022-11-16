package io.vyne.utils

import com.google.common.base.Stopwatch
import com.google.common.collect.HashMultimap
import java.time.Duration

class TimeBucketed {
   private val activities = HashMultimap.create<String,Duration>()
   companion object {
      val DEFAULT:TimeBucketed = TimeBucketed()
   }
   fun addActivity(key:String, duration:Duration) {
      activities.put(key,duration)
   }
   fun log() {
      activities.asMap().forEach { (key,durations) ->
         val average = durations.map { it.toMillis() }.average()
         val total = durations.map { it.toMillis() }.sum()
         println("Activity: $key; Count: ${durations.size};  Total: $total ms;  Average: $average ms  ")
      }
   }
}



suspend fun <T> timeBucket(name: String, bucket:TimeBucketed = TimeBucketed.DEFAULT, lambda: suspend () -> T):T {
   val sw = Stopwatch.createStarted()
   val result = lambda()
   bucket.addActivity(name, sw.elapsed())
   return result
}

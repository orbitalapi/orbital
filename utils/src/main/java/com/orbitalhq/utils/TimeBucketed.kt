package com.orbitalhq.utils

import com.google.common.base.Stopwatch
import com.google.common.collect.HashMultimap
import java.time.Duration

class TimeBucketed {
   private val activities = HashMultimap.create<String,Duration>()
   companion object {
      // var as we want to enable time bucketing in our performance tests.
      var ENABLED = false
      val DEFAULT: TimeBucketed = TimeBucketed()
   }
   fun addActivity(key:String, duration:Duration) {
      if (ENABLED) {
         activities.put(key, duration)
      }
   }
   fun log() {
      activities.asMap().forEach { (key,durations) ->
         val average = durations.map { it.toMillis() }.average()
         val total = durations.sumOf { it.toMillis() }
         println("Activity: $key;Count: ${durations.size};Total:;$total;Average:;$average")
      }
   }
}


fun <T> timeBucket(name: String, bucket:TimeBucketed = TimeBucketed.DEFAULT, lambda: () -> T):T {
   val sw = Stopwatch.createStarted()
   val result = lambda()
   bucket.addActivity(name, sw.elapsed())
   return result
}


suspend fun <T> timeBucketAsync(name: String, bucket:TimeBucketed = TimeBucketed.DEFAULT, lambda: suspend () -> T):T {
   val sw = Stopwatch.createStarted()
   val result = lambda()
   bucket.addActivity(name, sw.elapsed())
   return result
}

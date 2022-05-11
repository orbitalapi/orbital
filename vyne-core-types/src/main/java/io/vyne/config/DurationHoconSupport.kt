package io.vyne.config

import com.typesafe.config.Config
import io.github.config4k.ClassContainer
import io.github.config4k.CustomType
import io.github.config4k.toConfig
import mu.KotlinLogging
import java.time.Duration

private val logger = KotlinLogging.logger {}
object DurationHoconSupport : CustomType {
   override fun parse(clazz: ClassContainer, config: Config, name: String): Any? {
      val raw = config.getString(name)
      return try {
         Duration.parse(raw)
      } catch (e:Exception) {
         logger.error(e) { "Could not parse $raw to a duration (config for key $name).  Use a valid format, such as P3D (for 3 days) or PT11H (for 11 hours).  See https://en.wikipedia.org/wiki/ISO_8601#Durations for valid formats"}
         null
      }
   }

   override fun testParse(clazz: ClassContainer): Boolean {
      return clazz.mapperClass == Duration::class
   }

   override fun testToConfig(obj: Any): Boolean {
     return obj is Duration
   }

   override fun toConfig(obj: Any, name: String): Config {
      return obj.toString().toConfig(name)
   }
}

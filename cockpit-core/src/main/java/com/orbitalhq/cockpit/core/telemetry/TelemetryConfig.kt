package com.orbitalhq.cockpit.core.telemetry

import com.orbitalhq.licensing.License
import com.orbitalhq.spring.utils.versionOrDev
import com.orbitalhq.telemetry.AnalyticsMeta
import com.orbitalhq.telemetry.NoopTelemetryService
import com.orbitalhq.telemetry.PosthogTelemetryService
import com.orbitalhq.telemetry.TelemetryService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TelemetryConfig {

   @Bean
   fun analyticsRecorder(
      license: License,
      buildInfo: BuildProperties? = null,
      @Value("\${spring.application.name}") applicationName: String,
      @Value("\${vyne.telemetry.enabled:true}") telemetryEnabled: Boolean = true,

      ): TelemetryService {
      val service = if (telemetryEnabled) {
         PosthogTelemetryService(
            license.licensee,
            AnalyticsMeta(
               productName = applicationName,
               version = buildInfo.versionOrDev(),
               hasLicense = !license.isFallbackLicense
            )
         )
      } else {
         NoopTelemetryService
      }
      service.record("$applicationName started")
      return service
   }
}

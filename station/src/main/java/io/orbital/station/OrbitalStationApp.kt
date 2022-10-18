package io.orbital.station

import io.orbital.station.lsp.LanguageServerConfig
import io.orbital.station.security.OrbitalUserConfig
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties

@SpringBootApplication
@EnableConfigurationProperties(
   LanguageServerConfig::class,
   OrbitalUserConfig::class
)
class OrbitalStationApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(OrbitalStationApp::class.java)
         app.setBannerMode(Banner.Mode.OFF)
         app.run(*args)
      }
   }
}

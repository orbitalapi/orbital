package com.orbitalhq.cockpit.core.content

import com.orbitalhq.spring.config.OrbitalOnly
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@ConfigurationProperties(prefix = "vyne.cms")
data class ContentSettings(
   val url: String? = null,
   val enabled: Boolean = true,
   val cacheDuration: Duration = Duration.ofHours(12)
)

@Configuration
@EnableConfigurationProperties(
   ContentSettings::class
)
@OrbitalOnly
class ContentSpringModule {

   @Bean
   fun defaultContent(): DefaultContentRepository {
      return DefaultContentRepository(
         listOf(
            ContentCardData(
               title = "Get Started",
               text = "Our onboarding wizard walks you through creating a new project, and adding your first data source",
               iconUrl = "/assets/img/tabler/building-lighthouse.svg",
               action = ContentCardAction(
                  "Get started",
                  route = listOf("onboarding")
               )
            ),
            ContentCardData(
               title = "Follow a tutorial",
               text = "Our guides take you through example projects, such as building a BFF, or working with Kafka",
               iconUrl = "/assets/img/tabler/building-lighthouse.svg",
               action = ContentCardAction(
                  "Explore our guides",
                  route = listOf("https://orbitalhq.com/docs/guides")
               )
            ),
            ContentCardData(
               title = "Learn Taxi",
               text = "Get to know Taxi - Orbital's query language - in the Taxi Plaground, an interactive tutorial",
               iconUrl = "/assets/img/tabler/building-lighthouse.svg",
               action = ContentCardAction(
                  "Visit Playground",
                  route = listOf("https://playground.taxilang.org")
               )
            ),
         )
      )
   }
}

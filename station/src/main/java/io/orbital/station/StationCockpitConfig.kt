package io.orbital.station

import io.vyne.cask.api.CaskApi
import io.vyne.cockpit.core.CockpitCoreConfig
import io.vyne.history.db.InProcessHistoryConfiguration
import io.vyne.history.rest.QueryHistoryRestConfig
import io.vyne.pipelines.jet.api.PipelineApi
import io.vyne.pipelines.jet.api.transport.PipelineJacksonModule
import io.vyne.query.runtime.core.EnableVyneQueryNode
import io.vyne.schemaServer.changelog.ChangelogApi
import io.vyne.schemaServer.editor.SchemaEditorApi
import io.vyne.schemaServer.packages.PackagesServiceApi
import io.vyne.schemaServer.repositories.RepositoryServiceApi
import io.vyne.search.embedded.EnableVyneEmbeddedSearch
import io.vyne.spring.EnableVyne
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import reactivefeign.spring.config.EnableReactiveFeignClients


@Configuration
@EnableVyne
@EnableVyneEmbeddedSearch
@EnableVyneQueryNode
@ComponentScan(basePackageClasses = [CockpitCoreConfig::class])
@Import(
   InProcessHistoryConfiguration::class,
   QueryHistoryRestConfig::class

)
class VyneConfig

@Configuration
class PipelineConfig {
   @Bean
   fun pipelineModule(): PipelineJacksonModule = PipelineJacksonModule()
}

@Configuration
@EnableReactiveFeignClients(
   clients = [
      CaskApi::class,
      PipelineApi::class,
      SchemaEditorApi::class,
      PackagesServiceApi::class,
      RepositoryServiceApi::class,
      ChangelogApi::class
   ]
)
class FeignConfig

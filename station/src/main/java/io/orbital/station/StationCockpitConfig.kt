package io.orbital.station

import com.orbitalhq.cockpit.core.CockpitCoreConfig
import com.orbitalhq.history.db.InProcessHistoryConfiguration
import com.orbitalhq.history.rest.QueryHistoryRestConfig
import com.orbitalhq.pipelines.jet.api.PipelineApi
import com.orbitalhq.pipelines.jet.api.transport.PipelineJacksonModule
import com.orbitalhq.query.runtime.core.EnableVyneQueryNode
import com.orbitalhq.schemaServer.changelog.ChangelogApi
import com.orbitalhq.schemaServer.editor.SchemaEditorApi
import com.orbitalhq.schemaServer.packages.PackagesServiceApi
import com.orbitalhq.schemaServer.repositories.RepositoryServiceApi
import com.orbitalhq.search.embedded.EnableVyneEmbeddedSearch
import com.orbitalhq.spring.EnableVyne
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
      PipelineApi::class,
      SchemaEditorApi::class,
      PackagesServiceApi::class,
      RepositoryServiceApi::class,
      ChangelogApi::class
   ]
)
class FeignConfig

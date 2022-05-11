package io.vyne.queryService.security

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.vyne.config.BaseHoconConfigFileRepository
import reactor.core.publisher.Flux
import java.nio.file.Path

class ConfigFileVyneUserRepository(path: Path, fallback: Config = ConfigFactory.systemProperties())
   : VyneUserRepository, BaseHoconConfigFileRepository<VyneUserConfigConfig>(path, fallback) {

   override fun findAll(): Flux<VyneUser> = Flux.fromIterable(this.typedConfig().vyneUserMap.values)

   override fun extract(config: Config): VyneUserConfigConfig = config.extract()

   override fun emptyConfig(): VyneUserConfigConfig  = VyneUserConfigConfig()
}

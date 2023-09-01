package io.vyne.auth.authentication

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.vyne.config.BaseHoconConfigFileRepository
import reactor.core.publisher.Flux
import java.nio.file.Path

@Deprecated("Use JPA instead")
class ConfigFileVyneUserRepository(path: Path, fallback: Config = ConfigFactory.systemEnvironment()) :
   VyneUserRepository, BaseHoconConfigFileRepository<VyneUserConfigConfig>(path, fallback) {

   override fun findAll(): Flux<VyneUser> = Flux.fromIterable(this.typedConfig().vyneUserMap.values)

   override fun extract(config: Config): VyneUserConfigConfig = config.extract()

   override fun emptyConfig(): VyneUserConfigConfig = VyneUserConfigConfig()
}

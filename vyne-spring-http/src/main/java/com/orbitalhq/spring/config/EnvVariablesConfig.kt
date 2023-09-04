package com.orbitalhq.spring.config

import com.orbitalhq.PackageIdentifier
import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Config settings defining where we should load env settings
 * in hocon files from.
 *
 * This supports hocon-specific variables, such as passwords etc.
 *
 * This class is in the wrong place - vyne-spring-http
 * as there's nothing http specific.
 *
 * However vyne-spring depends on vyne-spring-http,
 * so this appears to be the lowest-common-package.
 *
 * Keen to find another place for this to live.
 */
@ConfigurationProperties(prefix = "vyne")
data class EnvVariablesConfig(
   val envVariablesPath: Path = Paths.get("config/env.conf")
) {
   companion object {
      val PACKAGE_IDENTIFIER = PackageIdentifier.fromId("com.orbitalhq.config/env/1.0.0")
   }
}


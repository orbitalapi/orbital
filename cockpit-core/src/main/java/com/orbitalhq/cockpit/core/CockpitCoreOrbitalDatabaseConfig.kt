package com.orbitalhq.cockpit.core

import com.orbitalhq.auth.authentication.VyneUser
import com.orbitalhq.spring.config.RequiresOrbitalDbEnabled
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@RequiresOrbitalDbEnabled
@Configuration
@EnableJpaRepositories
@EntityScan(basePackageClasses = [VyneUser::class, CockpitCoreConfig::class])
class CockpitCoreOrbitalDatabaseConfig
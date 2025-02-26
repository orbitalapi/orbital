package com.orbitalhq.spring.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@ConditionalOnProperty(prefix = "vyne.db", name = ["enabled"], havingValue = "true", matchIfMissing = false)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresOrbitalDbEnabled

@ConditionalOnProperty(prefix = "vyne.db", name = ["enabled"], havingValue = "false", matchIfMissing = false)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresOrbitalDbDisabled



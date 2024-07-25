package com.orbitalhq.spring.config

import org.springframework.context.annotation.Profile

/**
 * Indicates that a bean should only be provided when running inside of Orbital.
 * This excludes beans that are injected via whitelabel solutions
 */
@Retention(AnnotationRetention.RUNTIME)
@Profile(OrbitalOnly.ORBITAL, "test")
annotation class OrbitalOnly {
   companion object {
      const val ORBITAL = "orbital"
   }
}

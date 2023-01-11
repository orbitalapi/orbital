package io.orbital.station

import org.springframework.context.annotation.Profile

@Retention(AnnotationRetention.RUNTIME)
@Profile("orbital")
annotation class IncludeInOrbitalStationOnly

@Retention(AnnotationRetention.RUNTIME)
@Profile("!orbital")
annotation class IncludeInVyneOnly

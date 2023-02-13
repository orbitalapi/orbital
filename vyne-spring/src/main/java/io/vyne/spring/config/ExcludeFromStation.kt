package io.vyne.spring.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass

@ConditionalOnMissingClass("io.orbital.station.OrbitalStationApp")
annotation class ExcludeFromOrbitalStation

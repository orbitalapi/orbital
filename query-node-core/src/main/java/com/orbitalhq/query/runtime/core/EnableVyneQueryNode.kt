package com.orbitalhq.query.runtime.core

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Import(VyneQueryNodeConfiguration::class)
annotation class EnableVyneQueryNode {
}

@Configuration
@ComponentScan
class VyneQueryNodeConfiguration

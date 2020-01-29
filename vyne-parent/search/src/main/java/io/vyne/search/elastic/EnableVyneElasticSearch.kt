package io.vyne.search.elastic

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Import(VyneElasticSearchConfiguration::class)
annotation class EnableVyneElasticSearch

@Configuration
@ComponentScan(basePackageClasses = [EnableVyneElasticSearch::class])
class VyneElasticSearchConfiguration

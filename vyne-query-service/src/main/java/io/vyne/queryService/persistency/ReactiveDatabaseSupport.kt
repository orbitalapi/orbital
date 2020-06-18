package io.vyne.queryService.persistency

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import io.r2dbc.spi.ConnectionFactory
import io.vyne.queryService.persistency.entity.QueryHistoryRecordRepository
import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.flyway.FlywayProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.CustomConversions.StoreConversions
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions
import org.springframework.data.r2dbc.dialect.PostgresDialect
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories


@Configuration
@EnableConfigurationProperties(FlywayProperties::class)
@ConditionalOnExpression("!T(org.springframework.util.StringUtils).isEmpty('\${spring.r2dbc.url:}')")
@EnableR2dbcRepositories("io.vyne.queryService.persistency.entity.QueryHistoryRecordEntity")
class ReactiveDatabaseSupport {
   private val objectMapper = jacksonObjectMapper()
      .registerModule(JavaTimeModule())
      .registerModule(Jdk8Module())
      .registerModule(ParameterNamesModule())
      .registerModule(JaxbAnnotationModule())
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

   @Bean
   fun r2dbcCustomConversions(): R2dbcCustomConversions {
      val customConverters: MutableList<Converter<*, *>?> = mutableListOf()
      // Don't change next three lines! if you do so, you'll spend days to figure out
      // what's wrong, until you've found PostgresMappingR2dbcConverterUnitTests in
      //https://github.com/spring-projects/spring-data-r2dbc/commit/df7919a98d99ff3e1189eecbc1ad065845d3cd7f
      val converters = R2dbcCustomConversions.STORE_CONVERTERS.toMutableList()
      converters.addAll(PostgresDialect.INSTANCE.converters)
      val storeConversions = StoreConversions
         .of(PostgresDialect.INSTANCE.simpleTypeHolder, converters)

      customConverters.add(QueryHistoryRecordReadingConverter(objectMapper))
      customConverters.add(QueryHistoryRecordWritingConverter(objectMapper))
      return R2dbcCustomConversions(storeConversions, customConverters)
   }


   @Bean(initMethod = "migrate")
   fun flyway(flywayProperties: FlywayProperties): Flyway {
      return Flyway(Flyway.configure()
         .baselineOnMigrate(true)
         .dataSource(flywayProperties.url, flywayProperties.user, flywayProperties.password)
      )
   }

   @Bean
   fun history(repository: QueryHistoryRecordRepository, connectionFactory: ConnectionFactory) = DatabaseBackedQueryHistory(repository, connectionFactory, QueryHistoryRecordReadingConverter(objectMapper))
}

package io.vyne.queryService.schemas.importing.database

import io.vyne.connectors.jdbc.DatabaseMetadataService
import io.vyne.connectors.jdbc.SimpleJdbcConnectionFactory
import io.vyne.connectors.jdbc.TableTaxiGenerationRequest
import io.vyne.connectors.jdbc.registry.JdbcConnectionRegistry
import io.vyne.queryService.schemas.importing.SchemaConversionRequest
import io.vyne.queryService.schemas.importing.SchemaConverter
import io.vyne.schema.api.SchemaProvider
import lang.taxi.generators.GeneratedTaxiCode
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

@Component
class DbTableSchemaConverter(
   private val connectionRegistry: JdbcConnectionRegistry,
   private val schemaProvider: SchemaProvider
) : SchemaConverter<DbTableSchemaConverterOptions> {

   companion object {
      const val FORMAT = "databaseTable"
   }

   override val supportedFormats: List<String> = listOf(FORMAT)
   override val conversionParamsType: KClass<DbTableSchemaConverterOptions> = DbTableSchemaConverterOptions::class

   override fun convert(
      request: SchemaConversionRequest,
      options: DbTableSchemaConverterOptions
   ): Mono<GeneratedTaxiCode> {
      return Mono.create { sink ->
         val connectionConfiguration = this.connectionRegistry.getConnection(options.connectionName)
         val template = SimpleJdbcConnectionFactory().jdbcTemplate(connectionConfiguration)
         val taxi = DatabaseMetadataService(template.jdbcTemplate)
            .generateTaxi(
               options.tables, schemaProvider.schema, options.connectionName
            )
         sink.success(taxi)
      }

   }
}

data class DbTableSchemaConverterOptions(
   val connectionName: String,
   val tables: List<TableTaxiGenerationRequest>
)

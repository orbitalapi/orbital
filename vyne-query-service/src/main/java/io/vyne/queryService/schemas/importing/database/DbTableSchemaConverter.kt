package io.vyne.queryService.schemas.importing.database

import io.vyne.connectors.jdbc.DatabaseMetadataService
import io.vyne.connectors.jdbc.DefaultJdbcTemplateProvider
import io.vyne.connectors.jdbc.TableTaxiGenerationRequest
import io.vyne.connectors.jdbc.registry.JdbcConnectionRegistry
import io.vyne.queryService.schemas.importing.SchemaConversionRequest
import io.vyne.queryService.schemas.importing.SchemaConverter
import io.vyne.schemaStore.SchemaProvider
import lang.taxi.generators.GeneratedTaxiCode
import org.springframework.stereotype.Component
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
   ): GeneratedTaxiCode {
      val connectionConfiguration = this.connectionRegistry.getConnection(options.connectionName)
      val template = DefaultJdbcTemplateProvider(connectionConfiguration).build()
      val taxi = DatabaseMetadataService(template.jdbcTemplate)
         .generateTaxi(
            options.tables, schemaProvider.schema(), options.connectionName
         )
      return taxi
   }
}

data class DbTableSchemaConverterOptions(
   val connectionName: String,
   val tables: List<TableTaxiGenerationRequest>
)

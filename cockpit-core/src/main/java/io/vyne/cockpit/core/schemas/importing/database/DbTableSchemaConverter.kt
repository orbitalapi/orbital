package io.vyne.cockpit.core.schemas.importing.database

import io.vyne.cockpit.core.schemas.importing.*
import io.vyne.connectors.jdbc.DatabaseMetadataService
import io.vyne.connectors.jdbc.SimpleJdbcConnectionFactory
import io.vyne.connectors.jdbc.TableTaxiGenerationRequest
import io.vyne.connectors.jdbc.registry.JdbcConnectionRegistry
import io.vyne.schema.api.SchemaProvider
import io.vyne.utils.Ids
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
   ): Mono<SourcePackageWithMessages> {
      return Mono.create { sink ->
         val connectionConfiguration = this.connectionRegistry.getConnection(options.connectionName)
         val template = SimpleJdbcConnectionFactory().jdbcTemplate(connectionConfiguration)
         val generatedCode = DatabaseMetadataService(template.jdbcTemplate)
            .generateTaxi(
               options.tables, schemaProvider.schema, options.connectionName
            )

         val filename = if (options.tables.size > 1) {
            generatedImportedFileName(options.connectionName + "Tables")
         } else {
            generatedImportedFileName(options.tables.single().table.tableName)
         }
         val sourcePackage = generatedCode.toSourcePackageWithMessages(
            request.packageIdentifier,
            filename
         )
         sink.success(sourcePackage)
      }

   }
}

data class DbTableSchemaConverterOptions(
   val connectionName: String,
   val tables: List<TableTaxiGenerationRequest>
)

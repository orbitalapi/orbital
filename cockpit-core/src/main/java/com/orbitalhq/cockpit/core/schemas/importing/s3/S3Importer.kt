package com.orbitalhq.cockpit.core.schemas.importing.s3

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.google.common.base.Throwables
import com.orbitalhq.cockpit.core.connectors.getConnectionConfigOrThrowNotFound
import com.orbitalhq.cockpit.core.schemas.editor.EditedSchema
import com.orbitalhq.cockpit.core.schemas.editor.generator.VyneSchemaToTaxiGenerator
import com.orbitalhq.cockpit.core.schemas.importing.SchemaConversionRequest
import com.orbitalhq.cockpit.core.schemas.importing.SchemaConverter
import com.orbitalhq.cockpit.core.schemas.importing.SourcePackageWithMessages
import com.orbitalhq.cockpit.core.schemas.importing.generatedImportedFileName
import com.orbitalhq.cockpit.core.schemas.importing.toSourcePackageWithMessages
import com.orbitalhq.connectors.aws.core.registry.AwsConnectionRegistry
import com.orbitalhq.connectors.aws.s3.S3ConnectorTaxi
import com.orbitalhq.connectors.aws.s3.buildS3Connection
import com.orbitalhq.formats.csv.CsvTaxiGenerator
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Operation
import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.QualifiedNameAsStringDeserializer
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.schemas.toTaxiQualifiedName
import com.orbitalhq.spring.http.BadRequestException
import com.orbitalhq.spring.http.NotFoundException
import lang.taxi.TaxiDocument
import lang.taxi.generators.GeneratedTaxiCode
import lang.taxi.generators.NamingUtils.replaceIllegalCharacters
import lang.taxi.generators.NamingUtils.toCapitalizedWords
import lang.taxi.services.OperationScope
import org.apache.commons.io.FilenameUtils
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import kotlin.reflect.KClass

import com.orbitalhq.connectors.aws.s3.S3ConnectorTaxi.FilenamePatternFqn
import lang.taxi.accessors.LiteralAccessor
import lang.taxi.expressions.LiteralExpression
import lang.taxi.types.CompilationUnit

data class S3ConverterOptions(
   val connectionName: String,
   val bucketName: String,
   @JsonDeserialize(using = QualifiedNameAsStringDeserializer::class)
   val fileSchemaType: QualifiedName,
   val targetNamespace: String = fileSchemaType.namespace,
   val filenamePattern: String = "*",
   val serviceName: String? = null,
   val operationName: String? = null,
   val fileToGenerateSchemaFrom: String? = null
)

private data class SchemaUnderConstruction(
   val types: Set<Type>,
   val returnType: Type,
   val schema: Schema
)

@Component
class S3Importer(private val schemaProvider: SchemaProvider, private val registry: AwsConnectionRegistry) :
   SchemaConverter<S3ConverterOptions> {
   companion object {
      const val SUPPORTED_FORMAT = "s3"
   }

   override val supportedFormats: List<String> = listOf(SUPPORTED_FORMAT)

   override val conversionParamsType: KClass<S3ConverterOptions> = S3ConverterOptions::class

   override fun convert(
      request: SchemaConversionRequest,
      options: S3ConverterOptions
   ): Mono<SourcePackageWithMessages> {
      val generator = S3ServiceGenerator()
      val schema = schemaProvider.schema

      val schemaUnderConstructionMono = if (options.fileToGenerateSchemaFrom != null) {
         generateModel(options)
            .map { types ->
               // Convert Taxi Types to Vyne Types by generating a schema
               val tempSchema = TaxiSchema(TaxiDocument(types, emptySet()), emptyList())
               val vyneTypes = tempSchema.types.filter { vyneType ->
                  types.any { taxiType -> taxiType.toQualifiedName().parameterizedName == vyneType.paramaterizedName }
               }.toSet()
               val modelType = vyneTypes.singleOrNull { it.name == options.fileSchemaType }
                  ?: error("After generating a schema for the payload, a type of ${options.fileSchemaType} was not found")
               SchemaUnderConstruction(
                  vyneTypes,
                  modelType,
                  tempSchema
               )
            }
      } else {
         val modelType = schema.typeOrNull(options.fileSchemaType)
            ?: throw NotFoundException("Type ${options.fileSchemaType.shortDisplayName} was not found in this schema. If you want to create this type, provide a sample payload")
         Mono.just(SchemaUnderConstruction(emptySet(), modelType, schema))
      }


      val serviceName = options.serviceName?.fqn() ?: S3Mapping.defaultServiceName(
         options.targetNamespace,
         options.connectionName
      )
      val operationName = options.operationName?.let { operationName ->
         OperationNames.name(
            serviceName.fullyQualifiedName,
            operationName
         ).fqn()
      } ?: S3Mapping.defaultOperationName(serviceName)
      val mappingRequest = S3Mapping(
         options.connectionName,
         options.bucketName,
         operationName,
         options.filenamePattern
      )
      return schemaUnderConstructionMono.map { schemaUnderConstruction ->
         generator.createServiceTaxi(
            mappingRequest,
            schemaUnderConstruction,
            schema
         ).toSourcePackageWithMessages(
            request.packageIdentifier,
            generatedImportedFileName(options.connectionName + '.' + supportedFormats[0])
         )
      }
   }

   private fun generateModel(options: S3ConverterOptions): Mono<Set<lang.taxi.types.Type>> {

      val connectionConfig = registry.getConnectionConfigOrThrowNotFound(options.connectionName)
      val connection = connectionConfig.buildS3Connection(options.bucketName)
      return connection.fetchAsInputStream(options.fileToGenerateSchemaFrom!!)
         .onErrorMap { exception ->
            val rootCause = Throwables.getRootCause(exception)
            if (rootCause is NoSuchBucketException) {
               throw NotFoundException("Bucket ${options.bucketName} does not exist")
            } else {
               throw exception
            }
         }
         .switchIfEmpty {
            throw NotFoundException("File ${options.fileToGenerateSchemaFrom} was not found in bucket ${options.bucketName}")
         }
         .single()
         .flatMap { fileNameAndInputStream ->
            val (_, inputStream) = fileNameAndInputStream
            inputStream
         }
         .map { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
         }
         .map { fileContents ->
            generateModelFromContents(options.fileToGenerateSchemaFrom, fileContents, options.fileSchemaType)
         }
   }

   private fun generateModelFromContents(
      fileName: String,
      fileContents: String,
      modelName: QualifiedName
   ): Set<lang.taxi.types.Type> {
      val extension = FilenameUtils.getExtension(fileName)
      return when (extension.lowercase()) {
         "csv", "tsv", "psv" -> CsvTaxiGenerator(fileContents, modelName.toTaxiQualifiedName()).generateTypes()
         else -> throw BadRequestException("Schema generation from $extension files is not supported. Please define this schema manually")
      }
   }
}

data class S3Mapping(
   val connectionName: String,
   val bucketName: String,
   val operationName: QualifiedName,
   val filenamePattern: String
) {
   companion object {
      fun defaultServiceName(namespace: String, connectionName: String): QualifiedName {
         return "$namespace.${connectionName.replaceIllegalCharacters().toCapitalizedWords()}Service".fqn()
      }

      fun defaultOperationName(serviceName: QualifiedName): QualifiedName {
         return OperationNames.name(
            serviceName.fullyQualifiedName,
            "read"
         ).fqn()
      }
   }
}


private class S3ServiceGenerator(
   private val taxiGenerator: VyneSchemaToTaxiGenerator = VyneSchemaToTaxiGenerator()
) {

   private fun createService(mapping: S3Mapping, filenamePattern: String, returnType: Type, schema: Schema): Service {
      val operation = Operation(
         mapping.operationName,
         parameters = listOf(
            Parameter(
               schema.type(FilenamePatternFqn),
               name = "filenamePattern",
               defaultValue = LiteralExpression(
                  LiteralAccessor(filenamePattern),
                  listOf(CompilationUnit.unspecified())
               ),
               nullable = false
            )
         ),
         returnType = returnType,
         metadata = listOf(
            S3ConnectorTaxi.Annotations.S3Operation(
               mapping.bucketName,
            ).asMetadata()
         ),
         sources = emptyList(),
         operationType = OperationScope.READ_ONLY
      )

      val service = Service(
         name = OperationNames.serviceName(mapping.operationName).fqn(),
         operations = listOf(operation),
         queryOperations = emptyList(),
         metadata = listOf(
            S3ConnectorTaxi.Annotations.S3Service(
               mapping.connectionName
            ).asMetadata()
         ),
         sourceCode = emptyList()
      )
      return service
   }

   fun createServiceTaxi(mapping: S3Mapping, schemaUnderConstruction: SchemaUnderConstruction, parentSchema: Schema): GeneratedTaxiCode {

      val arrayReturnType =
         schemaUnderConstruction.schema.type(schemaUnderConstruction.returnType.asArrayType().taxiType)
      val service = createService(mapping, mapping.filenamePattern, arrayReturnType, parentSchema)
      return taxiGenerator.generate(
         EditedSchema(
            types = schemaUnderConstruction.types,
            services = setOf(service)
         ),
         schemaUnderConstruction.schema.asTaxiSchema()
      )
   }
}

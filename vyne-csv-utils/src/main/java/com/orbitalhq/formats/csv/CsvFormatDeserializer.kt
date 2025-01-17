package com.orbitalhq.formats.csv

import com.orbitalhq.models.DataSource
import com.orbitalhq.models.InPlaceQueryEngine
import com.orbitalhq.models.ParsingFailureBehaviour
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.ValueSupplier
import com.orbitalhq.models.format.FormatRegistry
import com.orbitalhq.models.format.ModelFormatDeserializer
import com.orbitalhq.models.format.StreamingModelFormatDeserializer
import com.orbitalhq.models.functions.FunctionRegistry
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.utils.log
import lang.taxi.accessors.ColumnAccessor
import lang.taxi.types.FormatsAndZoneOffset
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import reactor.core.publisher.Flux
import java.io.InputStream
import java.util.stream.StreamSupport

object CsvFormatDeserializer : ModelFormatDeserializer, StreamingModelFormatDeserializer {
   override fun canParse(value: Any, metadata: Metadata, type: Type): Boolean {
      val isReadable = value is String || value is CSVRecord
      if (!isReadable) {
         return false
      }

      // If the type uses `by column(...)` then we can't parse it here.
      val isUsingLegacyColumnMappings = type.attributes.values.any { it.accessor is ColumnAccessor }
      return !isUsingLegacyColumnMappings
   }

   override fun parse(value: Any, type: Type, metadata: Metadata, schema: Schema, source: DataSource): Any {
      if (value is CSVRecord) {
         return value.toMap()
      }
      val csvAnnotation = CsvFormatSpecAnnotation.from(metadata)
      require(value is String)
      val format = CsvFormatFactory.fromParameters(csvAnnotation.ingestionParameters)
      val content = CsvImporterUtil.trimContent(value, csvAnnotation.ingestionParameters.preludeText)
      val parsed = CSVParser.parse(content, format)
      return parsed.records.map { record -> record.toMap() }
//      return records

   }

   override fun supportsStreamingForSource(value: Any): Boolean {
      return value is InputStream
   }

   override fun stream(
      value: Any,
      type: Type,
      schema: Schema,
      source: DataSource,
      functionRegistry: FunctionRegistry,
      formatRegistry: FormatRegistry,
      inPlaceQueryEngine: InPlaceQueryEngine?,
      parsingErrorBehaviour: ParsingFailureBehaviour,
      format: FormatsAndZoneOffset?,
      metadata: Map<String, Any>,
      valueSuppliers: List<ValueSupplier>
   ): Flux<TypedInstance> {
      val memberType = type.collectionType ?: type
      val csvSpecMetadata = memberType.getMetadata(CsvAnnotationSpec.NAME)
      val csvAnnotation = CsvFormatSpecAnnotation.from(csvSpecMetadata)
      val csvFormat = CsvFormatFactory.fromParameters(csvAnnotation.ingestionParameters)
      require(value is InputStream) { "Parsing CSV to a stream is not supported for input value of ${value::class.simpleName}" }
      val parsed = csvFormat.parse(value.bufferedReader())
      val typedInstanceStream = StreamSupport.stream(parsed.spliterator(), false).map { csvRecord ->
         TypedInstance.from(
            type = memberType,
            value = csvRecord,
            schema = schema,
            source = source,
            functionRegistry = functionRegistry,
            formatSpecs = formatRegistry.formats,
            inPlaceQueryEngine = inPlaceQueryEngine,
            parsingErrorBehaviour = parsingErrorBehaviour,
            format = format,
            metadata = metadata,
            valueSuppliers = valueSuppliers
         )
      }
      return Flux.fromStream(typedInstanceStream)
   }

}

package com.orbitalhq.formats.csv

import lang.taxi.TaxiDocument
import lang.taxi.generators.FieldName
import lang.taxi.generators.GeneratedTaxiCode
import lang.taxi.generators.Logger
import lang.taxi.generators.NamespacedType
import lang.taxi.generators.SchemaWriter
import lang.taxi.generators.TypeDefinitionHelper
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.Modifier
import lang.taxi.types.ObjectType
import lang.taxi.types.ObjectTypeDefinition
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ParserOptions
import org.jetbrains.kotlinx.dataframe.io.readDelim
import org.jetbrains.kotlinx.dataframe.name
import java.io.StringReader

class CsvTaxiGenerator(
   private val csv: String,
   val modelTypeName: QualifiedName,
   private val schemaWriter: SchemaWriter = SchemaWriter(),
   private val logger: Logger = Logger()
) {

   private val _generatedTypes = mutableMapOf<QualifiedName, Type>()

   fun generateSchema(): GeneratedTaxiCode {
      return generateSchemaAndCode().second
   }

   fun generateSchemaAndCode(): Pair<TaxiDocument, GeneratedTaxiCode> {
      val taxiDoc = TaxiDocument(generateTypes(), emptySet())
      val taxi = schemaWriter.generateSchemas(listOf(taxiDoc))
      return taxiDoc to GeneratedTaxiCode(taxi, logger.messages)
   }

   fun generateTypes(): Set<Type> {
      val ingestionParameters = CsvFormatDetector.detectFormat(csv)

      // Data frame seems to not like it when CSVFormat yeilds actual null values.
      // So, remove the nullValues from the fomrat, and pass them to the dataframe library instead.

      val csvFormat = CsvFormatFactory.fromParameters(ingestionParameters.copy(nullValue = emptySet()))
      val dataframe = DataFrame.readDelim(
         StringReader(csv),
         csvFormat,
         parserOptions = ParserOptions(nullStrings = ingestionParameters.nullValue)
      )
      val fields = dataframe.columns().map { column ->
         val primitiveType = CsvPrimitiveTypeDetector.inferTaxiPrimitive(column)
         val isNullable = column.values().any { it == null }
         val fieldType = createScalarType(column.name, primitiveType)

         Field(
            column.name,
            type = fieldType,
            nullable = isNullable,
            compilationUnit = CompilationUnit.unspecified()
         )
      }

      val model = ObjectType(
         modelTypeName.parameterizedName,
         ObjectTypeDefinition(
            fields.toSet(),
            annotations = setOf(ingestionParameters.toAnnotation().asAnnotation(TaxiDocument.empty())),
            modifiers = listOf(Modifier.CLOSED),
            compilationUnit = CompilationUnit.unspecified()
         )
      )
      _generatedTypes.put(modelTypeName, model)
      return _generatedTypes.values.toSet()
   }

   private fun createScalarType(name: String, primitiveType: Type): Type {
      val qualifiedName = TypeDefinitionHelper.forHint(NamespacedType(modelTypeName.namespace, modelTypeName.typeName))
         .append(FieldName(name))
         .suggestName()
      return _generatedTypes.getOrPut(qualifiedName) {
         ObjectType(
            qualifiedName.parameterizedName,
            ObjectTypeDefinition(
               inheritsFrom = listOf(primitiveType),
               compilationUnit = CompilationUnit.unspecified()
            )
         )
      }
   }


}

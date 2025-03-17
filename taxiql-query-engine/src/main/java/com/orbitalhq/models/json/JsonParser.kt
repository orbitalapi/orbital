package com.orbitalhq.models.json

import arrow.core.Either
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.orbitalhq.ModelContainer
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.models.functions.FunctionRegistry
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.fqn

object RelaxedJsonMapper {
   val jackson: ObjectMapper = jacksonObjectMapper()
      .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
}

/**
 * Be careful when calling addJson(), and using projections.
 * Objects added here are "globally scoped", and can accidentally be pulled into projections.
 * Oftentimes it's better to stub a service, which is more closely aligned with actual production code.
 */
fun ModelContainer.addJson(typeName: String, json: String, source: DataSource = Provided): TypedInstance {
   val instance = TypedInstance.from(this.getType(typeName), json, this.schema, source = source)
   addModel(instance)
   return instance
}

@Deprecated("Call TypedInstance.from() or ModelContainer.addJson() instead.  This method has bugs with nested objects, and does not handle accessors or advanced features.")
fun ModelContainer.addJsonModel(typeName: String, json: String, source: DataSource = Provided): TypedInstance {
   val model = parseJsonModel(typeName, json)
//   if (model is TypedCollection) {
//      model.forEach { this.addModel(it) }
//   } else {
//      this.addModel(model)
//   }
   this.addModel(model)

   return model
}


@Deprecated("Call TypedInstance.from() instead.  This method has bugs with nested objects, and does not handle accessors or advanced features.")
fun ModelContainer.parseJsonModel(typeName: String, json: String, source: DataSource = Provided): TypedInstance {
   val type = this.getType(typeName.fqn().parameterizedName)
   return jsonParser().parse(type, json, source = source, format = null)
}

fun parseJson(
   schema: Schema,
   typeName: String,
   json: String,
   source: DataSource = Provided,
   functionRegistry: FunctionRegistry = FunctionRegistry.default,
   metadata: Map<String, Any> = emptyMap(),
   formatSpecs: List<ModelFormatSpec> = emptyList(),
): TypedInstance {
   val type = schema.type(typeName.fqn().parameterizedName)
   return TypedInstance.from(type, json, schema, source = source, functionRegistry = functionRegistry, metadata = metadata, formatSpecs = formatSpecs)
}

fun ModelContainer.parseJson(
   typeName: String,
   json: String,
   source: DataSource = Provided,
   functionRegistry: FunctionRegistry = FunctionRegistry.default,
   metadata: Map<String, Any> = emptyMap(),
   formatSpecs: List<ModelFormatSpec> = emptyList(),
): TypedInstance {
   return parseJson(this.schema, typeName, json, source, functionRegistry, metadata, formatSpecs = formatSpecs)
}

fun tryParseJson(
   schema: Schema,
   typeName: String,
   json: String,
   source: DataSource = Provided,
   functionRegistry: FunctionRegistry = FunctionRegistry.default,
   metadata: Map<String, Any> = emptyMap(),
   formatSpecs: List<ModelFormatSpec> = emptyList(),
): Either<StreamErrorMessage, TypedInstance> {
   val type = schema.type(typeName.fqn().parameterizedName)
   return TypedInstance.tryFrom(type, json, schema, source = source, functionRegistry = functionRegistry, metadata = metadata, formatSpecs = formatSpecs)
}
fun ModelContainer.tryParseJson(
   typeName: String,
   json: String,
   source: DataSource = Provided,
   functionRegistry: FunctionRegistry = FunctionRegistry.default,
   metadata: Map<String, Any> = emptyMap(),
   formatSpecs: List<ModelFormatSpec> = emptyList(),
): Either<StreamErrorMessage, TypedInstance> {
   return try {
      Either.Right(
      parseJson(this.schema, typeName, json, source, functionRegistry, metadata, formatSpecs = formatSpecs))
   } catch (e: Exception) {
      Either.Left(StreamErrorMessage.fromException(e, typeName))
   }
}

fun TypedInstance.right() = Either.Right(this)

@Deprecated("Call TypedInstance.from() instead.  This method has bugs with nested objects, and does not handle accessors or advanced features.")
fun ModelContainer.parseJsonCollection(
   typeName: String,
   json: String,
   source: DataSource = Provided
): List<TypedInstance> {
   val typedCollection =
      jsonParser().parse(this.getType(typeName.fqn().parameterizedName), json, source = source, format = null) as TypedCollection
   return typedCollection.value
}

fun ModelContainer.fromTypedCollection(typeName: String,
                                     json: String,
                                     source: DataSource = Provided): List<Either.Right<TypedInstance>> {
  return (TypedInstance.from(
      type = this.getType(typeName.fqn().parameterizedName),
      value = json,
      schema = this.schema,
      source = source
   ) as TypedCollection).value.map { Either.Right(it) }
}

fun ModelContainer.jsonParser(mapper: ObjectMapper = RelaxedJsonMapper.jackson): JsonModelParser {
   return JsonModelParser(this.schema, mapper)
}


package com.orbitalhq.models.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.orbitalhq.ModelContainer
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.functions.FunctionRegistry
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
   functionRegistry: FunctionRegistry = FunctionRegistry.default
): TypedInstance {
   val type = schema.type(typeName.fqn().parameterizedName)
   return TypedInstance.from(type, json, schema, source = source, functionRegistry = functionRegistry)
}

fun ModelContainer.parseJson(
   typeName: String,
   json: String,
   source: DataSource = Provided,
   functionRegistry: FunctionRegistry = FunctionRegistry.default
): TypedInstance {
   return parseJson(this.schema, typeName, json, source, functionRegistry)
}

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

fun ModelContainer.jsonParser(mapper: ObjectMapper = RelaxedJsonMapper.jackson): JsonModelParser {
   return JsonModelParser(this.schema, mapper)
}


package io.vyne.models.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.ModelContainer
import io.vyne.models.DataSource
import io.vyne.models.Provided
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.functions.FunctionRegistry
import io.vyne.schemas.fqn

object RelaxedJsonMapper {
   val jackson: ObjectMapper = jacksonObjectMapper()
      .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
}

fun ModelContainer.addJson(typeName: String, json: String, source:DataSource = Provided): TypedInstance {
   val instance = TypedInstance.from(this.getType(typeName), json, this.schema, source = source)
   addModel(instance)
   return instance
}
@Deprecated("Call TypedInstance.from() or ModelContainer.addJson() instead.  This method has bugs with nested objects, and does not handle accessors or advanced features.")
fun ModelContainer.addJsonModel(typeName: String, json: String, source:DataSource = Provided): TypedInstance {
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
fun ModelContainer.parseJsonModel(typeName: String, json: String, source:DataSource = Provided): TypedInstance {
   val type = this.getType(typeName.fqn().parameterizedName)
   return jsonParser().parse(type, json, source = source)
}
fun ModelContainer.parseJson(typeName: String, json: String, source:DataSource = Provided, functionRegistry: FunctionRegistry = FunctionRegistry.default): TypedInstance {
   val type = this.getType(typeName.fqn().parameterizedName)
   return TypedInstance.from(type, json, schema, source = source, functionRegistry = functionRegistry)
}

@Deprecated("Call TypedInstance.from() instead.  This method has bugs with nested objects, and does not handle accessors or advanced features.")
fun ModelContainer.parseJsonCollection(typeName: String, json: String, source:DataSource = Provided): List<TypedInstance> {
   val typedCollection = jsonParser().parse(this.getType(typeName.fqn().parameterizedName), json, source = source) as TypedCollection
   return typedCollection.value
}

fun ModelContainer.jsonParser(mapper: ObjectMapper = RelaxedJsonMapper.jackson): JsonModelParser {
   return JsonModelParser(this.schema, mapper)
}


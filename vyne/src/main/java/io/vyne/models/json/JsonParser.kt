package io.vyne.models.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.ModelContainer
import io.vyne.models.*
import io.vyne.schemas.fqn

object RelaxedJsonMapper {
   val jackson: ObjectMapper = jacksonObjectMapper()
      .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
}

@Deprecated("Call TypedInstance.from() instead.  This method has bugs with nested objects, and does not handle accessors or advanced features.")
fun ModelContainer.addJsonModel(typeName: String, json: String, source:DataSource = Provided): TypedInstance {
   val model = parseJsonModel(typeName, json)
   this.addModel(model)
   return model
}

fun ModelContainer.parseJsonModel(typeName: String, json: String, source:DataSource = Provided): TypedInstance {
   return jsonParser().parse(this.getType(typeName.fqn().parameterizedName), json, source = source)
}


fun ModelContainer.jsonParser(mapper: ObjectMapper = RelaxedJsonMapper.jackson): JsonModelParser {
   return JsonModelParser(this.schema, mapper)
}


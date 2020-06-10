package io.vyne.models.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.ModelContainer
import io.vyne.models.*
import io.vyne.schemas.Field
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import io.vyne.utils.log
import lang.taxi.types.PrimitiveType

object RelaxedJsonMapper {
   val jackson: ObjectMapper = jacksonObjectMapper()
      .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
}

fun ModelContainer.addJsonModel(typeName: String, json: String): TypedInstance {
   val model = parseJsonModel(typeName, json)
   this.addModel(model)
   return model
}

fun ModelContainer.parseJsonModel(typeName: String, json: String): TypedInstance {
   return jsonParser().parse(this.getType(typeName.fqn().parameterizedName), json)
}


fun ModelContainer.jsonParser(mapper: ObjectMapper = RelaxedJsonMapper.jackson): JsonModelParser {
   return JsonModelParser(this.schema, mapper)
}


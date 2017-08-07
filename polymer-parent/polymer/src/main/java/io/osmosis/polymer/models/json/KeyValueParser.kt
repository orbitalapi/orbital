package io.osmosis.polymer.models.json

import io.osmosis.polymer.ModelContainer
import io.osmosis.polymer.models.TypedInstance
import lang.taxi.TypeNames

fun ModelContainer.addKeyValuePair(typeName: String, value: Any): ModelContainer {
   this.addModel(TypedInstance.from(this.getType(typeName), value, this.schema))
   return this
}

fun ModelContainer.parseKeyValuePair(typeName: String, value: Any): TypedInstance {
   return TypedInstance.from(this.getType(typeName), value, this.schema)
}

fun ModelContainer.addAnnotatedInstance(value: Any): ModelContainer {
   val typeName = TypeNames.deriveTypeName(value.javaClass)
   return addKeyValuePair(typeName, value)
}


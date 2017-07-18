package io.osmosis.polymer.models.json

import io.osmosis.polymer.Polymer
import io.osmosis.polymer.models.TypedInstance

fun Polymer.addKeyValuePair(typeName: String, value: Any) {
   this.addData(TypedInstance.from(this.getType(typeName), value, this.schema))
}


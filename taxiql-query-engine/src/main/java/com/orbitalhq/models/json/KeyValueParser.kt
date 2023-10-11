package com.orbitalhq.models.json

import com.orbitalhq.FactSetId
import com.orbitalhq.FactSets
import com.orbitalhq.ModelContainer
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import lang.taxi.TypeNames

fun ModelContainer.addKeyValuePair(typeName: String, value: Any, factSetId: FactSetId = FactSets.DEFAULT, source:DataSource = Provided): ModelContainer {
   this.addModel(TypedInstance.from(this.getType(typeName), value, this.schema, source = source), factSetId)
   return this
}

fun ModelContainer.parseKeyValuePair(typeName: String, value: Any, source:DataSource = Provided): TypedInstance {
   return TypedInstance.from(this.getType(typeName), value, this.schema, source = source)
}

fun ModelContainer.addAnnotatedInstance(value: Any, factSetId: FactSetId = FactSets.DEFAULT): ModelContainer {
   val typeName = TypeNames.deriveTypeName(value.javaClass)
   return addKeyValuePair(typeName, value, factSetId)
}




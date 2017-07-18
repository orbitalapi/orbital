package io.osmosis.polymer.schemas


data class Parameter(val type: Type, val metadata: List<Metadata> = emptyList())

data class Operation(val name: String, val parameters: List<Parameter>, val returnType: Type, val metadata: List<Metadata> = emptyList()) {
   fun metadata(name: String): Metadata {
      return metadata.firstOrNull { it.name.fullyQualifiedName == name } ?: throw IllegalArgumentException("$name not present within this metataa")
   }
}

data class Service(val qualifiedName: String, val operations: List<Operation>, val metadata: List<Metadata> = emptyList()) {
   fun operation(name: String): Operation {
      return this.operations.first { it.name == name }
   }
}


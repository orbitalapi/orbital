package com.orbitalhq.schemas

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.collect.MapMaker
import lang.taxi.types.ArrayType
import lang.taxi.types.QualifiedNameParser
import java.io.Serializable

@kotlinx.serialization.Serializable
data class QualifiedName @Deprecated("call QualifiedName.from() instead, as it uses a pool of instances") constructor(
   val fullyQualifiedName: String,
   val parameters: List<QualifiedName> = emptyList()
) :
   Serializable {

   companion object {
      private val POOL = MapMaker().makeMap<String, QualifiedName>()
      fun from(namespace: String, name: String, parmeters: List<QualifiedName> = emptyList()): QualifiedName {
         return if (namespace.isNotBlank()) {
            from("$namespace.$name", parmeters)
         } else {
            from(name, parmeters)
         }

      }

      fun from(fullyQualifiedName: String, parameters: List<QualifiedName> = emptyList()): QualifiedName {
         return POOL.getOrPut(calculateParameterizedName(fullyQualifiedName, parameters)) {
            QualifiedName(fullyQualifiedName, parameters)
         }
      }

      fun calculateParameterizedName(fullyQualifiedName: String, parameters: List<QualifiedName>): String {
         return if (parameters.isEmpty()) {
            fullyQualifiedName
         } else {
            val params = parameters.joinToString(",") { it.parameterizedName }
            "$fullyQualifiedName<$params>"
         }
      }

   }

   @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
   val name: String = fullyQualifiedName.split(".").last()

   val parameterizedName: String = calculateParameterizedName(fullyQualifiedName, parameters)

   @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
   val namespace: String = fullyQualifiedName.split(".").dropLast(1).joinToString(".")

   // Convenience for the UI
   // Note: don't use by lazy {} here, as we see
   // a lot of allocation of QualifiedName (even with the interner)
   // and the lazy {} seems to allocate a bunch of bytes that drive up the heap
   @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
   val longDisplayName: String
      get() {
         val longTypeName = this.parameterizedName.replace("@@", " / ")
         return when {
            this.fullyQualifiedName == ArrayType.NAME && parameters.size == 1 -> parameters[0].fullyQualifiedName + "[]"
            this.parameters.isNotEmpty() -> longTypeName + this.parameters.joinToString(
               ",",
               prefix = "<",
               postfix = ">"
            ) { it.longDisplayName }

            else -> longTypeName
         }
      }

   // Convenience for the UI
   // Note: don't use by lazy {} here, as we see
   // a lot of allocation of QualifiedName (even with the interner)
   // and the lazy {} seems to allocate a bunch of bytes that drive up the heap
   @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
   val shortDisplayName: String
      get() {
         val shortTypeName = this.name.split("@@").last()
         return when {
            this.fullyQualifiedName == ArrayType.NAME && parameters.size == 1 -> parameters[0].shortDisplayName + "[]"
            this.parameters.isNotEmpty() -> shortTypeName + this.parameters.joinToString(
               ",",
               prefix = "<",
               postfix = ">"
            ) { it.shortDisplayName }

            else -> shortTypeName
         }
      }

   override fun toString(): String = parameterizedName
   override fun equals(other: Any?): Boolean {
      if (other == null) return false
      if (other === this) return true
      if (other !is QualifiedName) return false
      return this.parameterizedName == other.parameterizedName
   }

   override fun hashCode(): Int {
      return parameterizedName.hashCode()
   }
}

fun lang.taxi.types.QualifiedName.toVyneQualifiedName(): QualifiedName {
   return this.parameterizedName.fqn()
}

fun QualifiedName.toTaxiQualifiedName(): lang.taxi.types.QualifiedName {
   return lang.taxi.types.QualifiedName.from(this.parameterizedName)
}

fun String.fqn(): QualifiedName {

   return when {
      OperationNames.isName(this) -> QualifiedName.from(this, emptyList())
      ParamNames.isParamName(this) -> QualifiedName.from(
         "param/" + ParamNames.typeNameInParamName(this).fqn().parameterizedName
      )

      else -> {
         val taxiQualifiedName = QualifiedNameParser.parse(this)
         QualifiedName.from(
            taxiQualifiedName.fullyQualifiedName,
            taxiQualifiedName.parameters.map { it.toVyneQualifiedName() }
         )
      }
   }

}


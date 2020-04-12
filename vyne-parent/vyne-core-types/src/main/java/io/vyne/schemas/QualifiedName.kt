package io.vyne.schemas

import java.io.Serializable

data class QualifiedName(val fullyQualifiedName: String, val parameters: List<QualifiedName> = emptyList()) : Serializable {
   val name: String
      get() = fullyQualifiedName.split(".").last()

   val parameterizedName: String
      get() {
         return if (parameters.isEmpty()) {
            fullyQualifiedName
         } else {
            val params = this.parameters.joinToString(",") { it.parameterizedName }
            "$fullyQualifiedName<$params>"
         }
      }

   fun rawTypeEquals(other: QualifiedName): Boolean {
      return this.fullyQualifiedName == other.fullyQualifiedName
   }

   val namespace: String
      get() {
         return fullyQualifiedName.split(".").dropLast(1).joinToString(".")
      }

   override fun toString(): String = fullyQualifiedName
}

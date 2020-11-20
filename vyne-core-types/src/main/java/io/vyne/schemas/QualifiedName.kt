package io.vyne.schemas

import lang.taxi.types.ArrayType
import lang.taxi.types.QualifiedNameParser
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

   // Convenience for the UI
   val longDisplayName: String
      get() {
         return if (this.fullyQualifiedName == ArrayType.NAME && parameters.size == 1) {
            parameters[0].fullyQualifiedName + "[]"
         } else {
            this.parameterizedName
         }
      }

   // Convenience for the UI
   val shortDisplayName: String
      get() {
         return if (this.fullyQualifiedName == ArrayType.NAME && parameters.size == 1) {
            parameters[0].shortDisplayName + "[]"
         } else {
            this.name
         }
      }

   override fun toString(): String = fullyQualifiedName
}

fun lang.taxi.types.QualifiedName.toVyneQualifiedName():QualifiedName {
   return this.parameterizedName.fqn()
}

fun String.fqn(): QualifiedName {

   return when {
      OperationNames.isName(this) -> QualifiedName(this, emptyList())
      ParamNames.isParamName(this) -> QualifiedName("param/" + ParamNames.typeNameInParamName(this).fqn().parameterizedName)
      else -> {
         val taxiQualifiedName = QualifiedNameParser.parse(this)
         QualifiedName(
            taxiQualifiedName.fullyQualifiedName,
            taxiQualifiedName.parameters.map { it.toVyneQualifiedName() }
         )
      }
   }

}


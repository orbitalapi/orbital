package com.orbitalhq.schemas


interface SchemaMember {
   val schemaMemberKind: SchemaMemberKind

   @Deprecated(message = "Workaround for https://gitlab.com/vyne/vyne/issues/34.  Will be removed")
   val memberQualifiedName: QualifiedName
      get() {
         return when (this) {
            is Type -> this.name
            is Service -> this.name
            is Operation -> this.qualifiedName
            is QueryOperation -> this.qualifiedName
            is TableOperation -> this.qualifiedName
            is StreamOperation -> this.qualifiedName
            else -> error("Unhandled SchemaMember type : ${this.javaClass.name}")
         }
      }
}

enum class SchemaMemberKind {
   SERVICE,
   TYPE,
   OPERATION,
   FIELD
}
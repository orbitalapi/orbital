package io.vyne.schemas

// A pointer to a type.
// Useful when parsing, and the type that we're referring to may not have been parsed yet.
// TODO : Move ConstraintProvider, since that's not an attribute of a TypeReference, and now we have fields
// TODO : Remove isCollection, and favour Array<T> types
data class TypeReference(val name: QualifiedName,
                         @Deprecated("Replace with lang.taxi.Array<T> types")
                         val isCollection: Boolean = false) {
   val fullyQualifiedName: String
      get() = name.fullyQualifiedName
}

package io.vyne.schemaStore

/**
 * Basic schema store that simply holds the schema set provided to it.
 * Use where schema validation is deferred elsewhere - ie., in a remote model
 */
class SimpleSchemaStore : SchemaStore {
   private var schemaSet: SchemaSet = SchemaSet.EMPTY
   fun setSchemaSet(value: SchemaSet) {
      this.schemaSet = value;
   }

   override fun schemaSet(): SchemaSet {
      return schemaSet
   }

   override val generation: Int
      get() {
         return schemaSet.generation
      }

}

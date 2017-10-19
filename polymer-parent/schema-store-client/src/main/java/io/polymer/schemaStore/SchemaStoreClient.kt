package io.polymer.schemaStore

interface SchemaStoreClient {
   fun submitSchema(schemaName: String,
                    schemaVersion: String,
                    schema: String)

   fun schemaSet():SchemaSet
}

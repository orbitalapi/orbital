package com.orbitalhq.models

import com.orbitalhq.schemas.Field
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type

/**
 * Supports providing values for constructing TypedInstance
 * in addition to the main Value used when parsing.
 * This is for things like passing headers / metadata / keys from operation
 * sources into the parse / build process
 */
interface ValueSupplier {
   fun canSupply(field: Field, fieldType: Type):Boolean
   fun supplyValue(
       field: Field,
       fieldType: Type,
       schema: Schema,
       source: DataSource,
       typedObjectFactory: TypedObjectFactory
   ): TypedInstance
}

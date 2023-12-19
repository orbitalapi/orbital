package com.orbitalhq.query

import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type

/**
 * A schema used in a query, which contains inline types.
 *
 * Most schema operations are forwrded to the underlying schema
 */
class QuerySchema(val inlineTypes: List<Type>, private val schema:Schema) : Schema by schema {
   private val inlineTypesByName = inlineTypes.associateBy { it.fullyQualifiedName }

   override fun type(name: String): Type {
      return inlineTypesByName[name] ?: schema.type(name)
   }

   override fun hasType(name: String): Boolean {
      return inlineTypesByName.containsKey(name) || schema.hasType(name)
   }
}

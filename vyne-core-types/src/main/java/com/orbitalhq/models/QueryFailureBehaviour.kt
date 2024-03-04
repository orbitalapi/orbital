package com.orbitalhq.models

import com.orbitalhq.schemas.Field
import com.orbitalhq.schemas.Type
import lang.taxi.types.Arrays

enum class QueryFailureBehaviour {

   /**
    * Throwing is the original behaviour. However, this becomes destructive
    * when throwing inside a mapping operation, as it kills the other
    * active mapping operations.
    */
   THROW,

   /**
    * If a query fails, send a typed null, with a DataSource of
    * FailedSearch.
    *
    * In this option, failed searches for collections always yield
    * null.
    *
    * This is less destructive than throwing an exception, but
    * can be ambiguous for consumers.
    */
   SEND_TYPED_NULL,

   /**
    * If the query fails, send a typed null (in the case of an object),
    * or an empty array (in the case of a collection).
    */
   SEND_TYPED_NULL_OR_EMPTY_ARRAY;

   companion object {
      fun defaultBehaviour(field:Field):QueryFailureBehaviour {
         return when {
            // For failed searches on arrays, favour an empty array.
            // See https://projects.notional.uk/youtrack/issue/ORB-236/
            !field.nullable && Arrays.isArray(field.type.parameterizedName) -> SEND_TYPED_NULL_OR_EMPTY_ARRAY
            field.nullable -> SEND_TYPED_NULL
            else -> THROW
         }
      }
      fun defaultBehaviour(type: Type):QueryFailureBehaviour {
         return when {
            Arrays.isArray(type.paramaterizedName) -> SEND_TYPED_NULL_OR_EMPTY_ARRAY
            else -> THROW
         }
      }
   }
}

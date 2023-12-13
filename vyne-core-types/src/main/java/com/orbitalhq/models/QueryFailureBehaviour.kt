package com.orbitalhq.models

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
    * This is less destructive than throwing an exception, but
    * can be ambiguous for consumers.
    */
   SEND_TYPED_NULL;
}

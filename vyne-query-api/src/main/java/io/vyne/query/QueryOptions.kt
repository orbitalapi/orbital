package io.vyne.query

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.models.json.Jackson
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.annotation

data class QueryOptions(
   /**
    * indciates that fields which are null
    * should not be serialized in the result.
    *
    * As a result, reponses may not satisfy the query contract
    * which expects fields to be present, and consumers should apply
    * leineint parsing.
    */
   val omitNulls: Boolean = false
) {

   /**
    * Indicates if these query options mean a custom mapper
    * should be used.
    */
   val requiresCustomMapper = omitNulls
   fun configure(mapper:ObjectMapper):ObjectMapper {
      if (omitNulls) {
         mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
      }
      return mapper
   }
   fun newObjectMapper():ObjectMapper {
      return configure(Jackson.newObjectMapperWithDefaults())
   }
   fun newObjectMapperIfRequired():ObjectMapper? {
      return if (requiresCustomMapper) {
         newObjectMapper()
      } else {
         null
      }
   }
   companion object {
      fun default() = QueryOptions()

      fun fromQuery(query: TaxiQlQuery): QueryOptions {
         return QueryOptions(
            omitNulls = query.annotation("OmitNulls") != null
         )
      }
   }
}

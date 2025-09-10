package com.orbitalhq.connectors.nosql.mongodb

import arrow.core.Either
import arrow.core.right
import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.connectors.nosql.mongodb.BuilderUtils.replaceParametersSafely
import org.bson.Document
import org.springframework.data.mongodb.core.query.BasicQuery

class MongoFilterBuilder(private val objectMapper: ObjectMapper) {
   fun buildFilter(
      filterTemplate: String,
      parameters: Map<String, Any?>
   ): Either<MalformedFilterException, BasicQuery> {

      return replaceParametersSafely(filterTemplate, parameters, 0, objectMapper)
         .mapLeft { MalformedFilterException(filterTemplate, it.message) }
         .map { processedFilter ->
            val document = Document.parse(processedFilter)
            BasicQuery(document)
         }

   }

   // Reuse exact same parameter replacement logic from MongoAggregateBuilder
}

class MalformedFilterException(
   val filter: String,
   override val message: String,
   override val cause: Throwable? = null
) : Exception(message, cause)

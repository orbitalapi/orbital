package com.orbitalhq.connectors

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schemas.Schema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import lang.taxi.CompilationException
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import mu.KotlinLogging

object TaxiQlInvokerUtils {
   private val logger = KotlinLogging.logger {}

   /**
    * Will parse the provided TaxiQL query string and return a TaxiQLQuery.
    * If the parse fails, returns a response intended for returning directly from an invoker,
    * which is a pre-configured flow of the error.
    *
    * This is then correctly (and gracefully) handled downstream.
    *
    * Use this by calling:
    *
    * ```
    *       val (query, _) = TaxiQlInvokerUtils.queryOrErrorFlow(schema, taxiQuery)
    *          .getOrElse { errorFlow ->
    *             return errorFlow
    *          }
    * ```
    */
   fun queryOrErrorFlow(schema: Schema, taxiQuery: TaxiQLQueryString):Either<Flow<Either<StreamErrorMessage, TypedInstance>>, TaxiQlQuery> {
      return try {
         val (query, _) = schema.parseQuery(taxiQuery)
         query.right()
      } catch (e: CompilationException) {
         logger.error { "Failed to parse the TaxiQL query for invoking query operation. Provided query was: $taxiQuery - errors: ${e.message}" }
         flowOf(StreamErrorMessage.fromException(e, VyneQlGrammar.QUERY_TYPE_NAME).left()).left()
      }
   }
}

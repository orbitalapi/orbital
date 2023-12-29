package com.orbitalhq.schemas

import com.google.common.base.Stopwatch
import com.google.common.cache.CacheBuilder
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.utils.log
import lang.taxi.Compiler
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery

/**
 * Compiles a TaxiQL query against the current schema.
 * Implementations may choose to cache
 */
interface QueryCompiler {
   /**
    * Parses a TaxiQL query using the current schema.
    * Returns a parsed TaxiQL Query, along with any explicit query options that were declared within the schema.
    * Also returns a schema, which is a superset of this schema, plus any anonymous types declared within the TaxiQL
    * schema.
    */
   fun compile(query: TaxiQLQueryString, useCache: Boolean = true): Triple<TaxiQlQuery, QueryOptions, TaxiSchema>
}

class DefaultQueryCompiler(private val schema: Schema, cacheSize: Long = 0) : QueryCompiler {
   private val queryCache = CacheBuilder.newBuilder()
      .maximumSize(cacheSize)
      .build<String, Triple<TaxiQlQuery, QueryOptions, TaxiSchema>>()

   override fun compile(query: TaxiQLQueryString, useCache: Boolean): Triple<TaxiQlQuery, QueryOptions, TaxiSchema> {
      fun compileQuery(): Triple<TaxiQlQuery, QueryOptions, TaxiSchema> {
         val sw = Stopwatch.createStarted()
         val taxiDoc = Compiler(source = query, importSources = listOf(this.schema.taxi)).compile()
         val taxiQlQuery = taxiDoc.queries.first()
         val taxiSchema = TaxiSchema(taxiDoc, this.schema.packages, this.schema.functionRegistry)
         val merged = taxiSchema.merge(this.schema.asTaxiSchema())
         log().debug("Compiled query in ${sw.elapsed().toMillis()}ms")
         return Triple(taxiQlQuery, QueryOptions.fromQuery(taxiQlQuery), merged)
      }

      return if (useCache) {
         queryCache.get(query) { compileQuery() }
      } else {
         compileQuery()
      }
   }
}

package com.orbitalhq.schemas

import com.google.common.base.Stopwatch
import com.google.common.cache.CacheBuilder
import com.google.common.util.concurrent.UncheckedExecutionException
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.utils.log
import lang.taxi.CompilationException
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
   fun compile(query: TaxiQLQueryString, useCache: Boolean = true): Triple<TaxiQlQuery, QueryOptions, Schema>
}

class DefaultQueryCompiler(private val schema: Schema, cacheSize: Long = 0) : QueryCompiler {
   private val queryCache = CacheBuilder.newBuilder()
      .maximumSize(cacheSize)
      .build<String, Triple<TaxiQlQuery, QueryOptions, Schema>>()

   override fun compile(query: TaxiQLQueryString, useCache: Boolean): Triple<TaxiQlQuery, QueryOptions, Schema> {
      fun compileQuery(): Triple<TaxiQlQuery, QueryOptions, Schema> {
         val sw = Stopwatch.createStarted()
         val taxiDoc = Compiler(source = query, importSources = listOf(this.schema.taxi)).compile()
         val taxiQlQuery = taxiDoc.queries.first()
         val taxiSchema = TaxiSchema(taxiDoc, this.schema.packages, this.schema.functionRegistry)
         val merged = taxiSchema.merge(this.schema.asTaxiSchema()).let { schema ->
            taxiQlQuery.serviceRestrictions.applyTo(schema)
         }

         log().debug("Compiled query in ${sw.elapsed().toMillis()}ms")
         return Triple(taxiQlQuery, QueryOptions.fromQuery(taxiQlQuery), merged)
      }

      return if (useCache) {
         try {
            queryCache.get(query) { compileQuery() }
         } catch (e:UncheckedExecutionException) {
            if (e.cause is CompilationException) {
               throw e.cause!!
            } else throw e
         }

      } else {
         compileQuery()
      }
   }
}

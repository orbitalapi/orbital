package com.orbitalhq.schemas

import com.google.common.base.Stopwatch
import com.google.common.cache.CacheBuilder
import com.orbitalhq.utils.log
import lang.taxi.Compiler
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery

/**
 * Compiles a TaxiQL query against the current schema.
 * Implementations may choose to cache
 */
interface QueryCompiler {
   fun compile(query: TaxiQLQueryString): Pair<TaxiQlQuery, QueryOptions>
}

class DefaultQueryCompiler(private val schema: Schema, cacheSize: Long = 0) : QueryCompiler {
   private val queryCache = CacheBuilder.newBuilder()
      .maximumSize(cacheSize)
      .build<String, Pair<TaxiQlQuery, QueryOptions>>()

   override fun compile(query: TaxiQLQueryString): Pair<TaxiQlQuery, QueryOptions> {
      val (compiledQuery, queryOptions) = queryCache.get(query) {
         val sw = Stopwatch.createStarted()
         val vyneQuery = Compiler(source = query, importSources = listOf(this.schema.taxi)).queries().first()
         log().debug("Compiled query in ${sw.elapsed().toMillis()}ms")
         vyneQuery to QueryOptions.fromQuery(vyneQuery)
      }
//      val sw = Stopwatch.createStarted()
//      val vyneQuery = Compiler(source = vyneQlQuery, importSources = listOf(this.schema.taxi)).queries().first()
//      log().debug("Compiled query in ${sw.elapsed().toMillis()}ms")
//      return vyneQuery to QueryOptions.fromQuery(vyneQuery)
      return compiledQuery to queryOptions
   }
}

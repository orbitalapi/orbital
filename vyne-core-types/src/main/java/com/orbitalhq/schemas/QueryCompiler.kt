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
   companion object {
      private const val COMPILED_QUERY_REFERENCE_PREFIX = ">> "

      /**
       * When passing a routed query, we historically used to pass the full string
       * and recompile it. However, that broke when we rejected duplicate symbols (as the query already exists
       * in the schema the query is compiled against).
       *
       * So, now we pass a magic token as a reference to the query.
       *
       * Longer-term, we could expose overloads to accept the query directly as an input
       * to the queryService
       */
      fun asCompiledQueryReference(query: TaxiQlQuery):TaxiQLQueryString {
         return "$COMPILED_QUERY_REFERENCE_PREFIX ${query.name.parameterizedName}"
      }
   }
   /**
    * Parses a TaxiQL query using the current schema.
    * Returns a parsed TaxiQL Query, along with any explicit query options that were declared within the schema.
    * Also returns a schema, which is a superset of this schema, plus any anonymous types declared within the TaxiQL
    * schema.
    */
   fun compile(query: TaxiQLQueryString, useCache: Boolean = true): Triple<TaxiQlQuery, QueryOptions, Schema>


   fun isCompiledQueryReference(query: TaxiQLQueryString): Boolean {
      return query.startsWith(COMPILED_QUERY_REFERENCE_PREFIX)
   }
   fun getQueryNameFromCompiledQueryReference(query: TaxiQLQueryString): QualifiedName {
      require(query.startsWith(COMPILED_QUERY_REFERENCE_PREFIX)) { "Provided string is not a reference to a compiled query"}
      return query.removePrefix(COMPILED_QUERY_REFERENCE_PREFIX).fqn()
   }
}

class DefaultQueryCompiler(private val schema: Schema, cacheSize: Long = 0) : QueryCompiler {
   private val queryCache = CacheBuilder.newBuilder()
      .maximumSize(cacheSize)
      .build<String, Triple<TaxiQlQuery, QueryOptions, Schema>>()

   override fun compile(query: TaxiQLQueryString, useCache: Boolean): Triple<TaxiQlQuery, QueryOptions, Schema> {
      fun compileQuery(): Triple<TaxiQlQuery, QueryOptions, Schema> {
         return if (isCompiledQueryReference(query)) {
            val queryName = getQueryNameFromCompiledQueryReference(query)
            val query = this.schema.query(queryName)

            Triple(query, QueryOptions.fromQuery(query), schema)
         } else {
            val sw = Stopwatch.createStarted()
            val taxiDoc = Compiler(source = query, importSources = listOf(this.schema.taxi)).compile()
            val taxiQlQuery = taxiDoc.queries.first()
            val taxiSchema = TaxiSchema(taxiDoc, this.schema.packages, this.schema.functionRegistry)
            val merged = taxiSchema.merge(this.schema.asTaxiSchema()).let { schema ->
               taxiQlQuery.serviceRestrictions.applyTo(schema)
            }

            log().debug("Compiled query in ${sw.elapsed().toMillis()}ms")
            Triple(taxiQlQuery, QueryOptions.fromQuery(taxiQlQuery), merged)
         }


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

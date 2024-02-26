package com.orbitalhq.connectors.jdbc.sql.dml

import org.jooq.Batch
import org.jooq.DSLContext
import org.jooq.Query
import org.jooq.Record
import org.jooq.Result
import org.jooq.ResultQuery

/**
 * A thin wrapper around jooq which could
 * be
 */
sealed interface SqlOperation {

   val returnsValues: StatementReturnsGeneratedValues
   fun execute(): Pair<Int, Result<Record>?>
   val sql: String
}

data class SqlBatch(val batch: Batch, override val sql: String) : SqlOperation {
   companion object {
      fun from(dsl: DSLContext, statements: List<out Query>): SqlBatch {
         val sql = statements.joinToString("\n") { it.sql }
         return SqlBatch(dsl.batch(statements), sql)
      }
   }

   override val returnsValues: StatementReturnsGeneratedValues = false

   override fun execute(): Pair<Int, Result<Record>?> {
      val size = batch.execute().fold(0) { acc, value -> acc + value }
      return size to null
   }

}

/**
 * Represents a single query.
 * The query itself may be a batch, if the underlying SQL engine supports it.
 * (eg: Postgres has better support for batching INSERT and UPSERT statements in a single SQL statement)
 */
data class SqlQuery(val query: Query, override val returnsValues: StatementReturnsGeneratedValues) : SqlOperation {
   init {
      if (returnsValues) {
         require(query is ResultQuery<*>) { "when returnsValues = true, query must be of type ResultQuery" }
      }
   }

   override fun execute(): Pair<Int, Result<Record>?> {
      return if (returnsValues) {
         val result = (query as ResultQuery<Record>).fetch()
         result.size to result
      } else {
         query.execute() to null
      }
   }

   override val sql: String
      get() = query.toString()

}


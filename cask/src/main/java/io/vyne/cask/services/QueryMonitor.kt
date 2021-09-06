package io.vyne.cask.services

import com.google.common.cache.CacheBuilder
import io.vyne.cask.config.CaskQueryDispatcherConfiguration
import io.vyne.cask.ingest.CaskMutationDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.Executors

private val logger = KotlinLogging.logger {}

/**
 * Class QueryMonitor
 *
 * Maps between row mutated messages from cask and any registered monitor
 * mutated -> [resolve to row] -> optionally emit to shared flow
 */
@Component
class QueryMonitor(private val jdbcTemplate: JdbcTemplate?,
                   private val caskMutationDispatcher: CaskMutationDispatcher?,
                   private val caskQueryDispatcherConfiguration: CaskQueryDispatcherConfiguration
)  {

   /**
    * Mapping between monitored casks (table names) and a shared flow of rows
    */
   private val cache = CacheBuilder.newBuilder().weakValues().build<String, MutableSharedFlow<Map<String,Any>>>()

   private val vyneQlMonitorDispatcher = Executors.newFixedThreadPool(caskQueryDispatcherConfiguration.queryDispatcherPoolSize).asCoroutineDispatcher()

   init {
      caskMutationDispatcher?.fluxMutated?.subscribe {
         logger.debug{"Cask Mutation - ${it.tableName} ${it.identity}"}

         CoroutineScope(vyneQlMonitorDispatcher).launch {
            cache.getIfPresent(it.tableName)?.let { sharedFlow ->
               val row = fetchCaskRowByTableNameByIdentifier(it.tableName, it.identity[0].columnName, it.identity[0].value)
               val tableName = it.tableName

               row?.let {
                  logger.debug{"Cask Mutation Row - $row"}
                  updateNotify(tableName, row)
               }
            }
         }
      }
   }

   /**
    * Accept a notification that a row in a cask has changed
    *
    * @param tableName
    * @param row row data represented as a map of column-value map
    */
   suspend fun updateNotify(tableName: String, row:Map<String,Any>) {

      cache.getIfPresent(tableName)?.let {
         it.emit( row )
      }

   }

   /**
    * Register a updateChannel against the provided tableName
    *
    * @param tableName
    */
   fun registerCaskMonitor(tableName:String): Flow<Map<String,Any>> {
      return cache.get(tableName) { MutableSharedFlow() }
   }

   fun fetchCaskRowByTableNameByIdentifier(tableName: String, identifier:String, identifierValue: Any):Map<String,Any>? {
      var sql = """SELECT * FROM $tableName WHERE $identifier = ?"""
      try {
          sql = when(identifierValue) {
            is UUID -> """SELECT * FROM $tableName WHERE cask_raw_id::text = ?""".trim()
            else -> """SELECT * FROM $tableName WHERE $identifier = ?""".trim()
         }
         return jdbcTemplate?.queryForObject(
            sql, ColumnMapRowMapper(), identifierValue.toString().replace("'", "")
         )

      } catch (exception: Exception) {
         logger.error {"ERROR with ${exception.message} [${sql}] value [$identifierValue]" }
         return null
      }
   }
}


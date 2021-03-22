package io.vyne.queryService.persistency

import io.r2dbc.postgresql.codec.Json
import io.r2dbc.spi.ConnectionFactory
import io.vyne.models.TypeNamedInstance
import io.vyne.query.history.QueryHistoryRecord
import io.vyne.queryService.QueryHistory
import io.vyne.queryService.persistency.entity.QueryHistoryRecordEntity
import io.vyne.queryService.persistency.entity.QueryHistoryRecordRepository
import io.vyne.utils.log
import org.springframework.data.r2dbc.core.DatabaseClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers


class DatabaseBackedQueryHistory(private val repository: QueryHistoryRecordRepository,
                                 private val connectionFactory: ConnectionFactory,
                                 private val queryHistoryRecordReadingConverter: QueryHistoryRecordReadingConverter
) : QueryHistory {
   override fun clear() {
      log().info("Clearing of history on db not yet implemented")
   }
   override fun add(recordProvider: () -> QueryHistoryRecord<out Any>) {
      val record = recordProvider()
      repository
         .save(QueryHistoryRecordEntity(queryId = record.id, record = record, timestamp = record.timestamp))
         .subscribeOn(Schedulers.parallel())
         .subscribe { log().info("Persisted query id => ${it.queryId}") }
   }

   override fun list(): Flux<QueryHistoryRecord<out Any>> {
      // ReactiveCrudRepository is really really poor compared to jpa based repos.
      // For instance, Pageable and Sortable query support incomplete and buggy.
      // TODO re-visit below when there are new rdbc2 releases....
      val client = DatabaseClient.builder().connectionFactory(connectionFactory).build()
      return client.execute("SELECT * FROM query_history_records ORDER BY executed_at DESC LIMIT 10")
         .map { row, _ -> row.get(2) }
         .all()
         .map { queryHistoryRecordReadingConverter.convert(it as Json)!! }
   }

   override fun get(id: String): Mono<QueryHistoryRecord<out Any>> {
      // TODO tests must be reviewed.
      val subItems : MutableMap<String, TypeNamedInstance> = mutableMapOf()
      return repository.findByQueryId(id).map { fromDb ->
         val results = fromDb.record.response.results
         if (results?.size == 1 &&
            results.values.first() != null &&
            results.values.first() is List<*>) {
            val typeNamedInstanceList =  results.values.first() as List<Map<String, Any?>>?
            val value =  typeNamedInstanceList?.map { typedNameInstance ->
               if (typedNameInstance.containsKey("value") && typedNameInstance.containsKey("source") && typedNameInstance.containsKey("typeName")) {
                  val z = typedNameInstance["value"] as Map<String, Map<String, Any?>>
                  z.keys.forEach {
                     if(z[it]?.containsKey("typeName")!! && z[it]?.containsKey("value")!!) {
                        val data = TypeNamedInstance(z[it]?.get("typeName")?.toString()!!, z[it]?.get("value")!!)
                        subItems[it] = data
                     }
                  }
                  TypeNamedInstance(typedNameInstance["typeName"].toString(), subItems)

               } else {
                  typedNameInstance
               }
            }

            if (value != null) {
               val modifiedResponse = fromDb.record.response.copy(results = mapOf<String, Any?>(Pair(results.keys.first(), value)))
               fromDb.record.withResponse(modifiedResponse)
            } else {
               fromDb.record
            }
         } else {
            fromDb.record
         }
      }
   }
}

package io.vyne.history.api

import io.vyne.history.rest.QueryResultNodeDetail
import io.vyne.history.rest.RegressionPackRequest
import io.vyne.query.QueryProfileData
import io.vyne.query.ValueWithTypeName
import io.vyne.query.history.LineageRecord
import io.vyne.query.history.QuerySummary
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

@ReactiveFeignClient("\${vyne.queryHistoryService.name:vyne-history-server}")
interface QueryHistoryServiceRestApi {
    @GetMapping("/api/query/history")
    fun listHistory(): Flux<QuerySummary>

    @GetMapping("/api/query/history/summary/clientId/{clientId}")
     fun getQuerySummary(@PathVariable("clientId") clientQueryId: String): Mono<QuerySummary>

    @GetMapping("/api/query/history/calls/{remoteCallId}")
      fun getRemoteCallResponse(@PathVariable("remoteCallId") remoteCallId: String): Mono<String>

    /**
        * Returns the results (as JSON of TypeNamedInstances) over server-sent-events
        */
    @GetMapping(
            "/api/query/history/{id}/results", produces = [
        MediaType.TEXT_EVENT_STREAM_VALUE,
        MediaType.APPLICATION_JSON_VALUE,
    ]
    )
       fun getHistoryRecordStream(
            @PathVariable("id") queryId: String,
            @RequestParam("limit", required = false) limit: Long? = null
       ): Flux<ValueWithTypeName>

    @GetMapping("/api/query/history/clientId/{id}/dataSource/{rowId}/{attributePath}")
        fun getNodeDetailFromClientQueryId(
            @PathVariable("id") clientQueryId: String,
            @PathVariable("rowId") rowValueHash: Int,
            @PathVariable("attributePath") attributePath: String
        ): Mono<QueryResultNodeDetail>

    @GetMapping("/api/query/history/{id}/dataSource/{rowId}/{attributePath}")
         fun getNodeDetail(
            @PathVariable("id") queryId: String,
            @PathVariable("rowId") rowValueHash: Int,
            @PathVariable("attributePath") attributePath: String
         ): Mono<QueryResultNodeDetail>

    @GetMapping("/api/query/history/clientId/{id}/profile")
            fun getQueryProfileDataFromClientId(@PathVariable("id") queryClientId: String): Mono<QueryProfileData>

    @GetMapping("/api/query/history/{id}/profile")
             fun getQueryProfileData(@PathVariable("id") queryId: String): Mono<QueryProfileData>

    @GetMapping("/api/query/history/dataSource/{id}")
              fun getLineageRecord(@PathVariable("id") dataSourceId: String): Mono<LineageRecord>

   /**
    * Modified to return Mono<byte[]> as the version writing into ServerHttpResponse
    * causes issues with Feign.
    */
    @PostMapping("/api/query/history/clientId/{id}/regressionPack")
    fun getRegressionPackFromClientId(
            @PathVariable("id") clientQueryId: String,
            @RequestBody request: RegressionPackRequest,
                ): Mono<ByteBuffer>

   /**
    * Modified to return Mono<byte[]> as the version writing into ServerHttpResponse
    * causes issues with Feign.
    */
   @PostMapping("/api/query/history/{id}/regressionPack")
                 fun getRegressionPack(
            @PathVariable("id") queryId: String,
            @RequestBody request: RegressionPackRequest,
                 ): Mono<ByteBuffer>
}

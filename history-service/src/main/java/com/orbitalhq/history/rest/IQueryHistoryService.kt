package com.orbitalhq.history.rest

import com.orbitalhq.history.api.QueryResultNodeDetail
import com.orbitalhq.history.api.RegressionPackRequest
import com.orbitalhq.history.rest.export.ExportFormat
import com.orbitalhq.query.QueryProfileData
import com.orbitalhq.query.ValueWithTypeName
import com.orbitalhq.query.history.LineageRecord
import com.orbitalhq.query.history.PartialRemoteCallResponse
import com.orbitalhq.query.history.QuerySankeyChartRow
import com.orbitalhq.query.history.QuerySummary
import org.springframework.http.server.reactive.ServerHttpResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

interface IQueryHistoryService {

    fun clearHistory()


    fun listHistory(): Flux<QuerySummary>


    fun getRemoteCallListByClientId(clientQueryId: String): Mono<List<PartialRemoteCallResponse>>


    fun getQuerySummary(clientQueryId: String): Mono<QuerySummary>


    fun getRemoteCallResponse(remoteCallId: String): Mono<String>

    /**
     * Returns the results (as JSON of TypeNamedInstances) over server-sent-events
     */

    fun getHistoryRecordStream(
        queryId: String,
        limit: Long?
    ): Flux<ValueWithTypeName>


    fun getNodeDetailFromClientQueryId(
        clientQueryId: String,
        rowValueHash: Int,
        attributePath: String
    ): Mono<QueryResultNodeDetail>


    fun getNodeDetail(
        queryId: String,
        rowValueHash: Int,
        attributePath: String
    ): Mono<QueryResultNodeDetail>


    fun exportQueryResults(
        queryId: String,
        exportFormat: ExportFormat,
        serverResponse: ServerHttpResponse
    ): Mono<Void>


    fun exportQueryResultsFromClientId(
        clientQueryId: String,
        exportFormat: ExportFormat,
        serverResponse: ServerHttpResponse
    ): Mono<Void>


    fun exportQueryResultsToModelFormat(
        queryId: String,
        serverResponse: ServerHttpResponse
    ): Mono<Void>


    fun exportQueryResultsModelFormatFromClientId(
        clientQueryId: String,
        serverResponse: ServerHttpResponse
    ): Mono<Void>


    fun getQueryProfileDataFromClientId(queryClientId: String): Mono<QueryProfileData>


    fun getQueryProfileData(queryId: String): Mono<QueryProfileData>


    fun getLineageRecord(dataSourceId: String): Mono<LineageRecord>


    fun getQuerySankeyView(queryId: String): List<QuerySankeyChartRow>


    fun getQuerySankeyViewFromClientQueryId(queryClientId: String): List<QuerySankeyChartRow>

    fun getRegressionPackFromClientId(
        clientQueryId: String,
        request: RegressionPackRequest
    ): Mono<ByteBuffer>


    fun getRegressionPack(
        queryId: String,
        request: RegressionPackRequest
    ): Mono<ByteBuffer>


    fun fetchAllQueriesReturnType(fullyQualifiedTypeName: String): Mono<QueryList>
}
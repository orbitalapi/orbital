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
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.config.RequiresOrbitalDbDisabled
import kotlinx.coroutines.FlowPreview
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

@RequiresOrbitalDbDisabled
@FlowPreview
@RestController
/**
 * When Postgres is disabled, this class returns empty responses to the UI not to cause any http 404 issues.
 */
class QueryHistoryNoDbService: IQueryHistoryService {
    @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
    @DeleteMapping("/api/query/history")
    override fun clearHistory() {}

    @PreAuthorize("hasAuthority('${VynePrivileges.ViewQueryHistory}')")
    @GetMapping("/api/query/history")
    override fun listHistory(): Flux<QuerySummary> = Flux.empty()

    @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
    @GetMapping("/api/query/history/clientId/{clientId}/calls")
    override fun getRemoteCallListByClientId(clientQueryId: String): Mono<List<PartialRemoteCallResponse>> = Mono.empty()

    @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
    @GetMapping("/api/query/history/summary/clientId/{clientId}")
    override fun getQuerySummary(clientQueryId: String): Mono<QuerySummary> = Mono.empty()

    @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
    @GetMapping("/api/query/history/calls/{remoteCallId}")
    override fun getRemoteCallResponse(remoteCallId: String): Mono<String> = Mono.empty()

    @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
    @GetMapping(
        "/api/query/history/{id}/results", produces = [
            MediaType.TEXT_EVENT_STREAM_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
        ]
    )
    override fun getHistoryRecordStream(queryId: String, limit: Long?): Flux<ValueWithTypeName> = Flux.empty()

    @GetMapping("/api/query/history/clientId/{id}/dataSource/{rowId}/{attributePath}")
    @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
    override fun getNodeDetailFromClientQueryId(
        clientQueryId: String,
        rowValueHash: Int,
        attributePath: String
    ): Mono<QueryResultNodeDetail> = Mono.empty()

    @GetMapping("/api/query/history/{id}/dataSource/{rowId}/{attributePath}")
    @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
    override fun getNodeDetail(queryId: String, rowValueHash: Int, attributePath: String): Mono<QueryResultNodeDetail> = Mono.empty()

    @GetMapping("/api/query/history/{id}/{format}/export")
    @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
    override fun exportQueryResults(
        queryId: String,
        exportFormat: ExportFormat,
        serverResponse: ServerHttpResponse
    ): Mono<Void>  = Mono.empty()

    @GetMapping("/api/query/history/clientId/{id}/{format}/export")
    @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
    override fun exportQueryResultsFromClientId(
        clientQueryId: String,
        exportFormat: ExportFormat,
        serverResponse: ServerHttpResponse
    ): Mono<Void> = Mono.empty()

    @GetMapping("/api/query/history/{id}/export")
    @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
    override fun exportQueryResultsToModelFormat(queryId: String, serverResponse: ServerHttpResponse): Mono<Void> = Mono.empty()

    @GetMapping("/api/query/history/clientId/{id}/export")
    @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
    override fun exportQueryResultsModelFormatFromClientId(
        clientQueryId: String,
        serverResponse: ServerHttpResponse
    ): Mono<Void> = Mono.empty()

    @GetMapping("/api/query/history/clientId/{id}/profile")
    @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
    override fun getQueryProfileDataFromClientId(queryClientId: String): Mono<QueryProfileData> = Mono.empty()

    @GetMapping("/api/query/history/{id}/profile")
    @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
    override fun getQueryProfileData(queryId: String): Mono<QueryProfileData> = Mono.empty()

    @GetMapping("/api/query/history/dataSource/{id}")
    @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
    override fun getLineageRecord(dataSourceId: String): Mono<LineageRecord> = Mono.empty()

    @GetMapping("/api/query/history/{id}/sankey")
    @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
    override fun getQuerySankeyView(queryId: String): List<QuerySankeyChartRow> = emptyList()

    @GetMapping("/api/query/history/clientId/{id}/sankey")
    @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
    override fun getQuerySankeyViewFromClientQueryId(queryClientId: String): List<QuerySankeyChartRow> = emptyList()

    @PostMapping("/api/query/history/clientId/{id}/regressionPack")
    @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
    override fun getRegressionPackFromClientId(
        clientQueryId: String,
        request: RegressionPackRequest
    ): Mono<ByteBuffer> = Mono.empty()

    @PostMapping("/api/query/history/{id}/regressionPack")
    @PreAuthorize("hasAuthority('${VynePrivileges.ViewHistoricQueryResults}')")
    override fun getRegressionPack(queryId: String, request: RegressionPackRequest): Mono<ByteBuffer> = Mono.empty()

    @GetMapping("/api/query/history/filter/{responseType}")
    override fun fetchAllQueriesReturnType(fullyQualifiedTypeName: String): Mono<QueryList> = Mono.empty()
}
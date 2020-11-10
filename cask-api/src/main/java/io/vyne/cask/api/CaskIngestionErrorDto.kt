package io.vyne.cask.api

import java.time.Instant

data class CaskIngestionErrorDto(
   val caskMessageId: String,
   val createdAt: Instant,
   val fqn: String, val error: String)
data class CaskIngestionErrorDtoPage(val items: List<CaskIngestionErrorDto>, val currentPage: Long, val totalItem: Long, val totalPages: Long)
data class CaskIngestionErrorsRequestDto(val pageNumber: Int, val pageSize: Int, val searchStart: Instant, val searchEnd: Instant)

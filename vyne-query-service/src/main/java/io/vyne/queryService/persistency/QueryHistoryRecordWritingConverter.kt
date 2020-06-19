package io.vyne.queryService.persistency

import com.fasterxml.jackson.databind.ObjectMapper
import io.r2dbc.postgresql.codec.Json
import io.vyne.queryService.QueryHistoryRecord
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter

@WritingConverter
class QueryHistoryRecordWritingConverter(private val objectMapper: ObjectMapper): Converter<QueryHistoryRecord<out Any>, Json> {
   override fun convert(source: QueryHistoryRecord<out Any>): Json {
      return Json.of(objectMapper.writeValueAsString(source))
   }
}

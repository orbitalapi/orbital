package io.vyne.queryService.history.db

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.r2dbc.postgresql.codec.Json
import io.vyne.query.history.QueryHistoryRecord
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter

@ReadingConverter
class QueryHistoryRecordReadingConverter(private val objectMapper: ObjectMapper): Converter<Json, QueryHistoryRecord<out Any>> {
   override fun convert(json: Json): QueryHistoryRecord<out Any>? {
      return objectMapper.readValue(json.asString(), object : TypeReference<QueryHistoryRecord<out Any>>(){})
   }
}

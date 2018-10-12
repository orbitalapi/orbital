package io.vyne

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.mrbean.MrBeanModule
import io.vyne.query.Query
import io.vyne.query.QueryMode
import lang.taxi.TypeNames
import org.springframework.web.client.RestTemplate
import kotlin.reflect.KClass

class VyneClient(private val queryService: VyneQueryService) {
   constructor(queryServiceUrl: String) : this(HttpVyneQueryService(queryServiceUrl))

   fun given(vararg model: Any): VyneQueryBuilder {
      val facts = model.map { modelValue ->
         TypeNames.deriveTypeName(modelValue.javaClass) as TypeName to modelValue
      }.toMap()

      return VyneQueryBuilder(facts, queryService)
   }
}

class VyneQueryBuilder internal constructor(val facts: Map<TypeName, Any>, private val queryService: VyneQueryService) {

   inline fun <reified T : Any> discover(): T? {
      val response = query(T::class, QueryMode.DISCOVER)
      if (response.containsResultFor(T::class.java)) {
         return response.getResultFor(T::class)
      } else {
         return null;
      }
   }

   inline fun <reified T : Any> gather(): List<T> {
      val response = query(T::class, QueryMode.GATHER)
      if (response.containsResultFor(T::class.java)) {
         return response.getResultListFor(T::class)
      } else {
         return emptyList();
      }
   }

   fun <T : Any> query(targetType: KClass<T>, mode: QueryMode): QueryClientResponse {
      val desiredTypeName = TypeNames.deriveTypeName(targetType.java)
      val query = Query(
         desiredTypeName,
         facts,
         mode
      )
      val response = queryService.submitQuery(query)
      return response
   }
}

interface VyneQueryService {
   fun submitQuery(query: Query): QueryClientResponse
}


class HttpVyneQueryService(private val queryServiceUrl: String, private val restTemplate: RestTemplate = RestTemplate()) : VyneQueryService {
   override fun submitQuery(query: Query): QueryClientResponse {
      val queryResult = restTemplate.postForObject(
         "$queryServiceUrl/query",
         query,
         QueryClientResponse::class.java
      )
      return queryResult
   }

}

data class QueryClientResponse(
   val isFullyResolved: Boolean,
   val results: Map<TypeName, Any?>
) {
   fun containsResultFor(type: Class<*>): Boolean {
      val typeName = TypeNames.deriveTypeName(type)
      return results.containsKey(typeName)
   }

   fun <T : Any> getResultFor(type: KClass<T>, objectMapper: ObjectMapper = Jackson.objectMapper): T {
      val typeName = TypeNames.deriveTypeName(type.java)
      val result = this.results[typeName]!!
      val typedResult = jacksonObjectMapper().convertValue(result, type.java)
      return typedResult
   }

   fun <T : Any> getResultListFor(type: KClass<T>, objectMapper: ObjectMapper = Jackson.objectMapper): List<T> {
      val typeName = TypeNames.deriveTypeName(type.java)
      val result = this.results[typeName]!!
      val typeRef = objectMapper.typeFactory.constructArrayType(type.java)
      val typedResultArray = objectMapper.convertValue<Array<T>>(result, typeRef)
      return typedResultArray.toList()
   }
}
typealias TypeName = String

object Jackson {
   val objectMapper = ObjectMapper()
      .registerModule(MrBeanModule())
}

package io.vyne

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.mrbean.MrBeanModule
import io.vyne.query.Fact
import io.vyne.query.Query
import io.vyne.query.QueryMode
import lang.taxi.TypeNames
import lang.taxi.TypeReference
import org.springframework.web.client.RestTemplate
import java.lang.reflect.ParameterizedType
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass

class VyneClient(private val queryService: VyneQueryService, private val objectMapper: ObjectMapper = Jackson.objectMapper) {
   constructor(queryServiceUrl: String) : this(HttpVyneQueryService(queryServiceUrl))

   fun given(vararg model: Any): VyneQueryBuilder {
      val facts = model.map { modelValue ->
         when (modelValue) {
            is Fact -> modelValue
            else -> Fact(TypeNames.deriveTypeName(modelValue.javaClass), modelValue)
         }
      }.toList()

      return VyneQueryBuilder(facts, queryService, objectMapper)
   }

   inline fun <reified T : Any> discover(): T? {
      return given().discover()
   }
}

class VyneQueryBuilder internal constructor(val facts: List<Fact>, private val queryService: VyneQueryService, val objectMapper: ObjectMapper) {

   inline fun <reified T : Any> discover(): T? {
      val typeRef = object : TypeReference<T>() {}
      val typeName = TypeNames.deriveTypeName(typeRef)
      val response = query(typeName, QueryMode.DISCOVER)
      if (response.containsResultFor(typeRef)) {
         return response.getResultFor(typeRef, objectMapper)
      } else {
         return null;
      }
   }

   inline fun <reified T : Any> gather(): List<T> {
      val response = query(T::class, QueryMode.GATHER)
      if (response.containsResultFor(T::class.java)) {
         return response.getResultListFor(T::class, objectMapper)
      } else {
         return emptyList();
      }
   }

   fun <T : Any> query(targetType: KClass<T>, mode: QueryMode): QueryClientResponse {
      val desiredTypeName = TypeNames.deriveTypeName(targetType.java)
      return query(desiredTypeName, mode)
   }

   fun query(typeName: String, mode: QueryMode): QueryClientResponse {
      val query = Query(
         typeName,
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

   fun containsResultFor(typeRef: TypeReference<*>): Boolean {
      val typeName = TypeNames.deriveTypeName(typeRef)
      return results.containsKey(typeName)
   }

   fun containsResultFor(type: Class<*>): Boolean {
      val typeName = TypeNames.deriveTypeName(type)
      return results.containsKey(typeName)
   }

   fun <T : Any> getResultFor(typeRef: TypeReference<T>, objectMapper: ObjectMapper = Jackson.objectMapper): T {
      val typeName = TypeNames.deriveTypeName(typeRef)
      val result = this.results[typeName]!!
      val jacksonType = typeRef.jacksonRef()
      val typedResult = objectMapper.convertValue(result, jacksonType) as T
      return typedResult
   }

   fun <T : Any> getResultFor(type: KClass<T>, objectMapper: ObjectMapper = Jackson.objectMapper): T {
      val typeName = TypeNames.deriveTypeName(type.java)
      val result = this.results[typeName]!!
      val typedResult = objectMapper.convertValue(result, type.java)
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
   val objectMapper:ObjectMapper = jacksonObjectMapper()
      .registerModule(MrBeanModule())
}

private fun <T> TypeReference<T>.jacksonRef(): JavaType {
   val type = this.type as ParameterizedType
   val rawType = type.rawType as Class<*>
   val args = type.actualTypeArguments.map { (it as WildcardType).upperBounds[0] as Class<*> }.toTypedArray()
   val jacksonType = TypeFactory.defaultInstance().constructParametricType(rawType, *args)
   return jacksonType
}

package io.vyne

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.mrbean.MrBeanModule
import io.vyne.query.*
import io.vyne.utils.log
import lang.taxi.TypeNames
import lang.taxi.TypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import java.lang.reflect.ParameterizedType
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass

class VyneClient(private val queryService: VyneQueryService, private val factProviders: List<FactProvider> = emptyList(), private val objectMapper: ObjectMapper = Jackson.objectMapper) : VyneQueryService by queryService {
   constructor(queryServiceUrl: String) : this(HttpVyneQueryService(queryServiceUrl))

   fun given(vararg model: Any): VyneQueryBuilder {
      val facts = model.map { modelValue ->
         when (modelValue) {
            is Fact -> modelValue
            else -> Fact(TypeNames.deriveTypeName(modelValue.javaClass), modelValue)
         }
      }.toList()

      val allFacts = this.factProviders.foldRight(facts) { provider, currentFacts -> currentFacts + provider.provideFacts(currentFacts) }

      return VyneQueryBuilder(allFacts, queryService, objectMapper)
   }

   inline fun <reified T : Any> discover(): T? {
      return given().discover()
   }

   /**
    * Discovers based on the provided type, but returns the objects untyped (typically either as a Map<>, or as a
    * list of Map).
    * This is useful when a policy is applied, which will cause the type's contracts to be broken (ie., non-nulls
    * returned as nulls, because of filtered values).
    */
   inline fun <reified T : Any> discoverUntyped(): Any? {
      return given().discoverUntyped<T>()
   }

}

class VyneQueryBuilder internal constructor(val facts: List<Fact>, private val queryService: VyneQueryService, val objectMapper: ObjectMapper) {

   inline fun <reified T : Any> discover(): T? {
      val typeRef = object : TypeReference<T>() {}
      val typeName = TypeNames.deriveTypeName(typeRef)
      val response = query(listOf(typeName), QueryMode.DISCOVER)
      return if (response.containsResultFor(typeRef)) {
         response.getResultFor(typeRef, objectMapper)
      } else {
         null;
      }

   }

   fun discover(typeName: TypeName) = discover(listOf(typeName))
   fun <T : Any> discover(typeClass: Class<T>): T? {
      val typeName = TypeNames.deriveTypeName(typeClass)
      val response = query(listOf(typeName), QueryMode.DISCOVER)
      return if (response.containsResultFor(typeClass)) {
         response.getResultFor(typeClass, objectMapper)
      } else {
         null
      }
   }

   fun discover(typeNames: List<TypeName>): Map<TypeName, Any?> {
      val result = query(typeNames, QueryMode.DISCOVER)
      return result.results
   }

   /**
    * Discovers based on the provided type, but returns the objects untyped (typically either as a Map<>, or as a
    * list of Map).
    * This is useful when a policy is applied, which will cause the type's contracts to be broken (ie., non-nulls
    * returned as nulls, because of filtered values).
    */
   inline fun <reified T : Any> discoverUntyped(): Any? {
      val typeRef = object : TypeReference<T>() {}
      val typeName = TypeNames.deriveTypeName(typeRef)
      val response = query(listOf(typeName), QueryMode.DISCOVER)
      return if (response.containsResultFor(typeRef)) {
         response.getUntypedResultFor(typeRef, objectMapper)
      } else {
         null;
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
      return query(listOf(desiredTypeName), mode)
   }

   fun query(typeNames: List<String>, mode: QueryMode): QueryClientResponse {
      val query = Query(
         TypeNameListQueryExpression(typeNames),
         facts,
         queryMode = mode)
      val response = queryService.submitQuery(query)
      return response
   }
}

interface VyneQueryService {
   fun submitQuery(query: Query, resultMode:ResultMode = ResultMode.SIMPLE): QueryClientResponse
   fun submitVyneQl(vyneQL: String, resultMode:ResultMode = ResultMode.SIMPLE): QueryClientResponse
   fun submitVyneQlForList(vyneQL: String): List<Map<String, Any?>>
}


class HttpVyneQueryService(private val queryServiceUrl: String, private val restTemplate: RestTemplate = RestTemplate()) : VyneQueryService {

   override fun submitQuery(query: Query, resultMode: ResultMode) = post("/api/query",resultMode, query)
   override fun submitVyneQl(vyneQL: String, resultMode: ResultMode) = post("/api/vyneql", resultMode, vyneQL)

   override fun submitVyneQlForList(vyneQL: String):List<Map<String, Any?>> {
      return emptyList()
   }

   private fun post(path: String, resultMode:ResultMode, body: Any): QueryClientResponse {
      val headers = HttpHeaders()
      headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      val query = HttpEntity(body, headers)
      return restTemplate.postForObject("$queryServiceUrl$path?resultMode=${resultMode.name}", query, QueryClientResponse::class.java)
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

   fun <T : Any> getUntypedResultFor(typeRef: TypeReference<T>, objectMapper: ObjectMapper = Jackson.objectMapper): Any {
      val typeName = TypeNames.deriveTypeName(typeRef)
      return results[typeName]!!;
   }

   fun <T : Any> getResultFor(typeRef: TypeReference<T>, objectMapper: ObjectMapper = Jackson.objectMapper): T {
      val typeName = TypeNames.deriveTypeName(typeRef)
      val result = this.results[typeName]!!
      val jacksonType = typeRef.jacksonRef()
      val typedResult = objectMapper.convertValue(result, jacksonType) as T
      return typedResult
   }

   fun <T : Any> getResultFor(type: KClass<T>, objectMapper: ObjectMapper = Jackson.objectMapper): T {
      return getResultFor(type.java, objectMapper)
   }

   fun <T : Any> getResultFor(type: Class<T>, objectMapper: ObjectMapper = Jackson.objectMapper): T {
      val typeName = TypeNames.deriveTypeName(type)
      val result = this.results[typeName]!!
      val typedResult = objectMapper.convertValue(result, type)
      return typedResult
   }


   fun <T : Any> getResultListFor(type: KClass<T>, objectMapper: ObjectMapper = Jackson.objectMapper): List<T> {
      val result = this.results[taxiListForType(type)]
      val resultList = result?.let {
         try {
            val typeRef = objectMapper.typeFactory.constructArrayType(type.java)
            val valueList = (result as List<Map<Any, Any>>).mapNotNull { it["value"] } as List<Map<String, Map<String, Any>>>
            if (valueList.isEmpty()) {
               return objectMapper.convertValue<Array<T>>(result, typeRef).toList()
            }
            // TODO this is very nasty hack to deserialise TypedInstanced data (which serialised as hashmap) into given pojo
            // However, the proper fix requires bringing additional dependencies (e.g. vyne-core-types) into vyne-client...
            val deserialisedMap =  valueList.map { entity ->
               entity.keys.map { attributeName ->
                  val attributeValue = entity[attributeName]?.get("value")
                  attributeName to attributeValue
               }.toMap()
            }
            val typedResultArray = objectMapper.convertValue<Array<T>>(deserialisedMap, typeRef)
            return typedResultArray.toList()
         } catch (e: Exception) {
            log().error("Error in getting result list for ${type.qualifiedName} from $results", e)
            emptyList<T>()
         }
      }
      return resultList ?: emptyList()
   }

   fun <T : Any> hasResultListFor(type: KClass<T>): Boolean = this.results.containsKey(taxiListForType(type))

   fun <T: Any> getResultMapListFor(type: Class<T>, replaceNullWith: Any? = null): List<Map<String, Any?>> {
      val untypedResults = this.results[taxiListForType(type)]
      return if (untypedResults != null) {
         val valueList = (untypedResults as List<Map<Any, Any>>).mapNotNull { it["value"] } as List<Map<String, Map<String, Any>>>
         valueList.map { entity ->
            entity.keys.map { attributeName ->
               val attributeValue = entity[attributeName]?.get("value")
               attributeName to (attributeValue ?: replaceNullWith)
            }.toMap()
         }
      } else {
         listOf()
      }
   }

   companion object {
      fun <T : Any> taxiListForType(type: KClass<T>) = "lang.taxi.Array<${type.qualifiedName}>"
      fun <T : Any> taxiListForType(type: Class<T>) = "lang.taxi.Array<${TypeNames.deriveTypeName(type)}>"
   }
}
typealias TypeName = String

object Jackson {
   val objectMapper: ObjectMapper = jacksonObjectMapper()
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,false)
      .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
      .registerModule(JavaTimeModule())
      .registerModule(MrBeanModule())
      .registerModule(vyneEnumDeserialisationModule())
}

private fun <T> TypeReference<T>.jacksonRef(): JavaType {
   val type = this.type as ParameterizedType
   val rawType = type.rawType as Class<*>
   val args = type.actualTypeArguments.map { (it as WildcardType).upperBounds[0] as Class<*> }.toTypedArray()
   val jacksonType = TypeFactory.defaultInstance().constructParametricType(rawType, *args)
   return jacksonType
}

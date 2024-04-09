package com.orbitalhq.connectors.nosql.mongodb

import com.orbitalhq.connectors.collectionTypeOrType
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.resultType
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.SqlExchange
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.AttributeName
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.Type
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import lang.taxi.query.TaxiQlQuery
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.Instant

abstract class MongoBaseInvoker(
   private val connectionFactory: MongoConnectionFactory,
   protected val schemaProvider: SchemaProvider
) {
   protected fun getConnectionConfigAndTemplate(service: Service): Pair<MongoConnectionConfiguration, ReactiveMongoTemplate> {
      val connectionName =
         service.firstMetadata(MongoConnector.Annotations.MongoOperation.NAME).params["connection"] as String
      val mongoConnectionConfig = connectionFactory.config(connectionName)
      return mongoConnectionConfig to connectionFactory.reactiveMongoTemplate(mongoConnectionConfig)
   }



   private fun objectIdField(vyneType: Type): AttributeName? {
      val idFields = vyneType.getAttributesWithAnnotation(MongoConnector.Annotations.ObjectIdAnnotationName)
      require(idFields.isEmpty() || idFields.size == 1)
      return if (idFields.isEmpty()) {
         null
      } else {
         idFields.keys.first()
      }
   }

   protected fun buildOperationResult(
      service: Service,
      operation: RemoteOperation,
      parameters: List<TypedInstance>,
      criteria: String,
      mongoHosts: String,
      elapsed: Duration,
      recordCount: Int,
      verb: String = "Query"
   ): OperationResult {

      val remoteCall = buildRemoteCall(service, mongoHosts, operation, criteria, elapsed, recordCount, verb)
      return OperationResult.fromTypedInstances(
         parameters,
         remoteCall
      )
   }

   protected fun buildRemoteCall(
      service: Service,
      mongoHosts: String,
      operation: RemoteOperation,
      criteria: String,
      elapsed: Duration,
      recordCount: Int,
      verb: String
   ) = RemoteCall(
      service = service.name,
      address = mongoHosts,
      operation = operation.name,
      responseTypeName = operation.returnType.name,
      requestBody = criteria,
      durationMs = elapsed.toMillis(),
      timestamp = Instant.now(),
      // If we implement streaming database queries, this will change
      responseMessageType = ResponseMessageType.FULL,
      // Feels like capturing the results are a bad idea.  Can revisit if there's a use-case
      response = null,
      exchange = SqlExchange(
         sql = criteria,
         recordCount = recordCount,
         verb = verb
      ),

      )

   protected fun convertToTypedInstances(
      resultList: Flux<Map<*, *>>,
      query: TaxiQlQuery,
      schema: Schema,
      datasource: DataSource
   ): Flow<TypedInstance> {
      val resultTypeName = query.resultType()
      val resultTaxiType = collectionTypeOrType(schema.taxi.type(resultTypeName))
      val vyneType = schema.type(resultTaxiType)
      val mapTransform: (Map<*, *>) -> Map<*, *> = objectIdFieldTransform(vyneType)

      val typedInstances = resultList
         .map { mapValue ->
            mapToTypedInstance(mapValue, vyneType, schema, datasource, mapTransform)
         }
      return typedInstances.asFlow()
   }

   protected fun mapToTypedInstance(
      mapValue: Map<*, *>, vyneType: Type,
      schema: Schema,
      datasource: DataSource,
      mapTransform: MongoIdTransformFunc? = null): TypedInstance {
      val idFieldTransform = mapTransform ?: objectIdFieldTransform(vyneType)
      return TypedInstance.from(
         vyneType,
         idFieldTransform(mapValue),
         schema,
         source = datasource,
         evaluateAccessors = false
      )
   }

   private fun objectIdFieldTransform(vyneType: Type): MongoIdTransformFunc {
      val idField = objectIdField(vyneType)
      val mapTransform: (Map<*, *>) -> Map<*, *> = if (idField != null) {
         fun(mongoMap: Map<*, *>): Map<*, *> {
            val hashMap = mongoMap as HashMap<String, Any?>
            hashMap.remove(MongoIdField)?.let { objectId ->
               hashMap[idField] = objectId.toString()
            }
            return hashMap
         }
      } else {
         { mongoMap: Map<*, *> -> mongoMap }
      }

      return mapTransform
   }


   protected fun typedInstanceToMap( recordToWrite: TypedInstance, idFieldCheck: Boolean = true): Map<String, Any?> {
      require(recordToWrite is TypedObject) { "Writes not supported on instances of type ${recordToWrite::class.simpleName}" }
      val idField = if (idFieldCheck) objectIdField(recordToWrite.type) else null
      return recordToWrite.type.attributes.map { (name, field) ->
         val fieldValue = recordToWrite[name]
         val value =  if (fieldValue is TypedObject) {
            typedInstanceToMap(fieldValue, false)
         } else {
            fieldValue.value
         }
         val mongoFieldName = if (idField == name) MongoIdField else name
         val mongoValue = if (mongoFieldName == MongoIdField)  {
           value?.let { ObjectId(it.toString()) }
         } else value
         mongoFieldName to mongoValue
      }.toMap()
   }

   companion object {
      const val MongoIdField = "_id"
      const val SelectAllCriteria = "{}"
   }
}

typealias MongoIdTransformFunc = (Map<*, *>) -> Map<*, *>

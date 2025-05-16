package com.orbitalhq.connectors.nosql.mongodb

import arrow.core.Either
import com.orbitalhq.connectors.collectionTypeOrType
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.resultType
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.ObjectMapperConfig
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.SqlExchange
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.AttributeName
import com.orbitalhq.schemas.OperationInvocationException
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import lang.taxi.query.TaxiQlQuery
import org.bson.types.Decimal128
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import reactor.core.publisher.Flux
import java.math.BigDecimal
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
      val idFields = vyneType.getAttributesWithAnnotation("Id".fqn())
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

   protected fun mapError(error: Throwable,
                        service: Service,
                        operation: RemoteOperation,
                         parameters: List<Pair<Parameter, TypedInstance>>,
                        criteria: String,
                        mongoHosts: String,
                        elapsed: Duration,
                        recordCount: Int,
                        verb: String = "Query"
   ): Throwable {
      val remoteCall = RemoteCall(
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
      response = error.message,
      exchange = SqlExchange(
         sql = criteria,
         recordCount = recordCount,
         verb = verb
      ),

      )
      return OperationInvocationException(
         "Failed to invoke service ${operation.name} at url $mongoHosts - ${error.message ?: "No message in instance of ${error::class.simpleName}"}",
         0,
         remoteCall,
         parameters
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
   ): Flow<Either<StreamErrorMessage, TypedInstance>>  {
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
      mapTransform: MongoIdTransformFunc? = null): Either<StreamErrorMessage, TypedInstance> {
      val idFieldTransform = mapTransform ?: objectIdFieldTransform(vyneType)
      return try {
          Either.Right(TypedInstance.from(
             vyneType,
             idFieldTransform(mapValue),
             schema,
             source = datasource,
             evaluateAccessors = false
          ))
        } catch (e: Exception) {
           Either.Left(StreamErrorMessage.fromException(e, vyneType.paramaterizedName))
        }
   }

   private fun objectIdFieldTransform(vyneType: Type): MongoIdTransformFunc {
      val idField = objectIdField(vyneType)
      val mapTransform: (Map<*, *>) -> Map<*, *> = if (idField != null) {
         fun(mongoMap: Map<*, *>): Map<*, *> {
            val hashMap = mongoMap as HashMap<String, Any?>
            hashMap.remove(MongoIdField)?.let { objectId ->
               hashMap[idField] = if (objectId is ObjectId)  objectId.toString() else objectId
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
            // When writing the object out, don't apply formats, as we want
            // dates persisted as dates, not strings
            fieldValue.toRawObject(ObjectMapperConfig(applyFormats = false))?.let { rawValue ->
               when (rawValue) {
                  // ORB-957
                  is BigDecimal -> Decimal128(rawValue)
                  else -> rawValue
               }
            }
         }
         val mongoFieldName = if (idField == name) MongoIdField else name
         val mongoValue = if (mongoFieldName == MongoIdField)  {
           value?.let {
              it
           }
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

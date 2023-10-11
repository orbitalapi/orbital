package com.orbitalhq.regression

import com.orbitalhq.http.UriVariableProvider
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.httpOperationMetadata
import com.orbitalhq.spring.http.DefaultRequestFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.http.HttpEntity
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

class ReplayingOperationInvoker(private val remoteCalls: List<RemoteCall>, private val schema: Schema) :
   OperationInvoker {
   private val uriVariableProvider = UriVariableProvider()
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      // TODO : We might wanna consider strict ordering when replaying.
      return findRecordedCall(operation) != null
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String
   ): Flow<TypedInstance> {
      val (_, url, _) = operation.httpOperationMetadata()
      val uriVariables = uriVariableProvider.getUriVariables(parameters, url)
      val path = UriComponentsBuilder.newInstance()
         .path(url)
         .buildAndExpand(uriVariables)
         .path
      val bodyFactory = DefaultRequestFactory()
      val requestBody = bodyFactory.buildRequestBody(operation, parameters.map { it.second })
      val recordedCall = findRecordedCall(operation, path, requestBody) ?: error("Expected a matching recorded call")
      val responseType = schema.type(recordedCall.responseTypeName)
      return flow {
         TypedInstance.from(
            responseType,
            recordedCall.response,
            schema,
            source = Provided,
            evaluateAccessors = false
         )
      }
   }

   private fun findRecordedCall(operation: RemoteOperation): RemoteCall? {
      return this.remoteCalls.firstOrNull {
         it.operationQualifiedName == operation.qualifiedName
      }
   }

   private fun findRecordedCall(
      operation: RemoteOperation,
      path: String,
      httpEntity: HttpEntity<*>
   ): RemoteCall? {
      return this.remoteCalls.firstOrNull {
         val remoteCallPath = UriComponentsBuilder.newInstance().uri(URI(it.address)).build().path
         val remoteCallBody = it.requestBody
         val operationBody = httpEntity.body
         it.operationQualifiedName == operation.qualifiedName && remoteCallPath == path && remoteCallBody == operationBody
      }
   }
}

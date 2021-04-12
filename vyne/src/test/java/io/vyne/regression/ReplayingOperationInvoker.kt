package io.vyne.regression

import io.vyne.http.UriVariableProvider
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.query.ProfilerOperation
import io.vyne.query.RemoteCall
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import io.vyne.schemas.httpOperationMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.http.HttpEntity
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

class ReplayingOperationInvoker(private val remoteCalls: List<RemoteCall>, private val schema: Schema) : OperationInvoker {
   private val uriVariableProvider = UriVariableProvider()
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      // TODO : We might wanna consider strict ordering when replaying.
      return findRecordedCall(operation) != null
   }

   override suspend fun invoke(service: Service,
                       operation: RemoteOperation,
                       parameters: List<Pair<Parameter, TypedInstance>>,
                       profilerOperation: ProfilerOperation, queryId: String?): Flow<TypedInstance> {
      val (_, url, _) = operation.httpOperationMetadata()
      val uriVariables = uriVariableProvider.getUriVariables(parameters, url)
      val path = UriComponentsBuilder.newInstance()
         .path(url)
         .buildAndExpand(uriVariables)
         .path
      val requestBody = UriVariableProvider.buildRequestBody(operation, parameters.map { it.second })
      val recordedCall = findRecordedCall(operation, path, requestBody) ?: error("Expected a matching recorded call")
      val responseType = schema.type(recordedCall.responseTypeName)
      return flow { TypedInstance.from(responseType, recordedCall.response, schema, source = Provided, evaluateAccessors = false) }
   }

   private fun findRecordedCall(operation: RemoteOperation): RemoteCall? {
      return this.remoteCalls.firstOrNull {
         it.operationQualifiedName == operation.qualifiedName
      }
   }

   private fun findRecordedCall(operation: RemoteOperation, path: String, body: Pair<HttpEntity<*>, Class<*>>): RemoteCall? {
      return this.remoteCalls.firstOrNull {
         val remoteCallPath = UriComponentsBuilder.newInstance().uri(URI(it.address)).build().path
         val remoteCallBody = it.requestBody
         val operationBody = body.first.body
         it.operationQualifiedName == operation.qualifiedName && remoteCallPath == path && remoteCallBody == operationBody
      }
   }
}

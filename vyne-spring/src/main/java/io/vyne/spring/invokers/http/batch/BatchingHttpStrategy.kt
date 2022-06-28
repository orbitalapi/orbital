package io.vyne.spring.invokers.http.batch

import io.vyne.models.TypedInstance
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Schema
import io.vyne.schemas.Service

interface BatchingHttpStrategy {
   fun findBatchingCandidate(
      operation: RemoteOperation,
      schema: Schema,
      service: Service,
      preferredParams: Set<TypedInstance>,
      providedParamValues: List<Pair<Parameter, TypedInstance>>
   ): BatchingOperationCandidate?
}


package com.orbitalhq.connectors.aws.s3

import arrow.core.Either
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.FormatRegistry
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow

/**
 * S3 invoker for mutating operations
 */
class S3WriteInvoker : BaseS3Invoker() {
   fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions,
      awsConnection: AwsConnectionConfiguration,
      bucketName: String,
      formatRegistry: FormatRegistry,
      schema: Schema
   ): Flow<Either<StreamErrorMessage, TypedInstance>>  {
      val filename = getFilenamePattern(parameters)
      val (_, body) = parameters.singleOrNull {
         it.first.hasMetadata(S3ConnectorTaxi.RequestBodyFqn.fullyQualifiedName)
      }
         ?: error("Expected exactly one input parameter to contain an annotation of @${S3ConnectorTaxi.RequestBodyFqn.fullyQualifiedName}")

      val (metadata, format) = formatRegistry.forType(body.type)
      val payload = if (format != null) {
         format.serializer.write(body, metadata!!, schema, 0)
         // ok to downgrade this to a log message if we understand a reasonable reason for it to happen
            ?: error("The format of type ${format::class.simpleName} produced a null payload against input of type ${body.type.name.longDisplayName}")
      } else {
         body.toRawObject()
            ?: error("Got null payload of type ${body.type.name.longDisplayName}")
      }
      return S3Connection(awsConnection, bucketName)
         .write(filename, payload)
         .map {
           Either.Right(body)
         }.asFlow()
   }


}

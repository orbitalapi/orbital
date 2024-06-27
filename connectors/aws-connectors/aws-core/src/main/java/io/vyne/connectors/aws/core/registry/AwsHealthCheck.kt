package io.vyne.connectors.aws.core.registry

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.orbitalhq.connectors.ConnectionSucceeded
import com.orbitalhq.connectors.aws.configureWithExplicitValuesIfProvided
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import mu.KotlinLogging
import software.amazon.awssdk.services.sts.StsClient

private val logger = KotlinLogging.logger {  }
fun AwsConnectionConfiguration.test(): Either<String, ConnectionSucceeded> {
    return try {
       val response = StsClient
           .builder()
           .configureWithExplicitValuesIfProvided(this)
           .build().use {
               /**
                * No permissions are required to perform this operation. If an administrator attaches a policy to the current identity that explicitly denies access to the sts:GetCallerIdentity action, we can still perform this operation.
                 */
               it.callerIdentity
       }

        logger.trace { "AWS health check response => account: ${response.account()}  userId: ${response.userId()}" }
        return ConnectionSucceeded.right()
    } catch (e: Exception) {
        "Error in AWS Connection ${this.connectionName}, error => ${e.message}".left()
    }
}
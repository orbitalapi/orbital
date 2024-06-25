package com.orbitalhq.annotations.http

import com.orbitalhq.VyneTypes
import com.orbitalhq.schemas.fqn

object HttpRetryAnnotationSchema {
   val namespace = "${VyneTypes.NAMESPACE}.http.operations"
   val NAME = "$namespace.HttpRetry"
   val FIXED_RETRY_POLICY_NAME = "$namespace.HttpFixedRetryPolicy"
   val EXPO_RETRY_POLICY_NAME = "$namespace.HttpExponentialRetryPolicy"

   val imports: String =  listOf(NAME, FIXED_RETRY_POLICY_NAME, EXPO_RETRY_POLICY_NAME).joinToString("\n") { "import $it" }

   val schema = """
namespace  $namespace {
   type RetryTimeInSeconds inherits Int
   type MaximumRetries inherits Int
   type ExponentialBackOffJitter inherits Decimal
   type ResponseCode inherits Int

   annotation ${FIXED_RETRY_POLICY_NAME.fqn().name} {
       maxRetries: MaximumRetries
       retryDelay: RetryTimeInSeconds
   }

   annotation ${EXPO_RETRY_POLICY_NAME.fqn().name} {
       maxRetries: MaximumRetries
       retryDelay: RetryTimeInSeconds
       jitter: ExponentialBackOffJitter = 0.5
   }

   annotation ${NAME.fqn().name} {
      fixedRetryPolicy: $FIXED_RETRY_POLICY_NAME?
      exponentialRetryPolicy: $EXPO_RETRY_POLICY_NAME?
      responseCode: ResponseCode[]
   }
}
"""

}






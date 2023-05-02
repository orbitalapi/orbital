package io.vyne.query.runtime.executor.serverless;

import io.vyne.query.runtime.CompressedQueryResultWrapper;
import io.vyne.query.runtime.QueryMessageCborWrapper;
import io.vyne.query.runtime.executor.QueryExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * Exposes serverless function mappings.  Used when running the node
 * as a lambda executor.
 *
 * Spring isn't detecting these functions when defined as Kotlin functions.
 * Hence this class is written in Java.
 */
@Configuration
@ConditionalOnProperty(value = "vyne.consumer.serverless.enabled", havingValue = "true", matchIfMissing = false)
public class FunctionRegistrationConfig {

   @Bean
   public Function<QueryMessageCborWrapper, CompressedQueryResultWrapper> queryFunction(QueryExecutor queryExecutor) {
      return queryExecutor::executeQuery;
   }
}

package io.vyne.query.runtime.executor.serverless;

import io.vyne.query.runtime.CompressedQueryResultWrapper;
import io.vyne.query.runtime.QueryMessageCborWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * Exposes serverless function mappings.  Used when running the node
 * as a lambda executor.
 * <p>
 * Spring isn't detecting these functions when defined as Kotlin functions.
 * Hence this class is written in Java.
 */
@Configuration
// Can't use ConditionalOnProperty when compiling to a Native image.
// It's fairly harmless exposing these functions even if nothing is invoking them.
//@ConditionalOnProperty(value = "vyne.consumer.serverless.enabled", havingValue = "true", matchIfMissing = false)
public class FunctionRegistrationConfig {

   @Bean
   public Function<QueryMessageCborWrapper, CompressedQueryResultWrapper> queryFunction(
      ServerlessQueryExecutor queryExecutor,
      ServerlessOverRabbitQueryExecutor rabbitQueryExecutor,
      @Value("${vyne.consumer.serverless.writeResponsesToQueue:false}") Boolean writeResponsesToQueue
   ) {
      if (writeResponsesToQueue) {
         return rabbitQueryExecutor::executeQuery;
      } else {
         return queryExecutor::executeQuery;
      }

   }
}


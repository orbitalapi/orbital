package io.vyne.query.runtime.http;

import io.vyne.query.runtime.CompressedQueryResultWrapper;
import io.vyne.query.runtime.QueryMessage;
import io.vyne.query.runtime.QueryMessageCborWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;

/**
 * Spring isn't detecting these functions when defined as Kotlin functions.
 * Hence this class is written in Java.
 */
@Configuration
public class FunctionConfig {

   @Bean
   public Function<QueryMessageCborWrapper, CompressedQueryResultWrapper> queryFunction(QueryExecutor queryExecutor) {
      return queryExecutor::executeQuery;
   }
}

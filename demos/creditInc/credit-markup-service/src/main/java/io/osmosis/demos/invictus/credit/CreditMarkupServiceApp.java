package io.osmosis.demos.invictus.credit;

import io.vyne.spring.EnableVyne;
import io.vyne.spring.RemoteSchemaStoreType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * App which calculates the markup to
 * add to an invoice as a cost of purchasing from a retailer.
 */
@SpringBootApplication
@EnableVyne(remoteSchemaStore = RemoteSchemaStoreType.HAZELCAST)
@EnableEurekaClient
public class CreditMarkupServiceApp {

   public static void main(String[] args) {
      SpringApplication.run(CreditMarkupServiceApp.class, args);
   }
}

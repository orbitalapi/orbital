package io.osmosis.demos.creditInc.isic;

import io.vyne.spring.EnableVyne;
import io.vyne.spring.RemoteSchemaStoreType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;


@SpringBootApplication
@EnableVyne(remoteSchemaStore = RemoteSchemaStoreType.HAZELCAST)
@EnableEurekaClient
public class IsicLookupServiceApp {
   public static void main(String[] args) {
      SpringApplication.run(IsicLookupServiceApp.class, args);
   }
}

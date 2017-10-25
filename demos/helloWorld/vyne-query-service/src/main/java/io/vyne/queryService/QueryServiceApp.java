package io.vyne.queryService;

import io.polymer.spring.EnablePolymer;
import io.polymer.spring.RemoteSchemaStoreType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
@EnablePolymer(remoteSchemaStore = RemoteSchemaStoreType.HAZELCAST)
public class QueryServiceApp {

   public static void main(String[] args) {
      SpringApplication.run(QueryServiceApp.class, args);
   }
}

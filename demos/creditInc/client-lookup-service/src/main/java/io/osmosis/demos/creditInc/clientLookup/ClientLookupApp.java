package io.osmosis.demos.creditInc.clientLookup;

import io.polymer.spring.EnablePolymer;
import io.polymer.spring.RemoteSchemaStoreType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnablePolymer(remoteSchemaStore = RemoteSchemaStoreType.HAZELCAST)
@EnableEurekaClient
public class ClientLookupApp {
   public static void main(String[] args) {
      SpringApplication.run(ClientLookupApp.class, args);
   }
}

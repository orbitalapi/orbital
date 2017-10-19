package io.osmosis.demos.helloWorld.greetingProvider;

import io.polymer.spring.EnablePolymer;
import io.polymer.spring.RemoteSchemaStoreType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@EnablePolymer(remoteSchemaStore = RemoteSchemaStoreType.HAZELCAST)
@EnableEurekaClient
@SpringBootApplication
public class GreetingProviderApp {

   public static void main(String[] args) {
      SpringApplication.run(GreetingProviderApp.class, args);
   }
}

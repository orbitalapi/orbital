package io.osmosis.demos.helloWorld.greetingProvider;

import io.vyne.spring.EnableVyne;
import io.vyne.spring.RemoteSchemaStoreType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@EnableVyne(remoteSchemaStore = RemoteSchemaStoreType.HAZELCAST)
@EnableEurekaClient
@SpringBootApplication
public class GreetingProviderApp {

   public static void main(String[] args) {
      SpringApplication.run(GreetingProviderApp.class, args);
   }
}

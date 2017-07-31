package io.osmosis.demos.creditInc;

import io.polymer.spring.schemaServer.EnableSchemaServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
@EnableSchemaServer
public class DiscoveryServer {
   public static void main(String[] args) {
      SpringApplication.run(DiscoveryServer.class, args);
   }
}

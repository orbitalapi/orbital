package io.osmosis.demos.creditinc.invoice;

import io.polymer.spring.EnablePolymer;
import io.polymer.spring.RemoteSchemaStoreType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnablePolymer(remoteSchemaStore = RemoteSchemaStoreType.HAZELCAST)
@EnableEurekaClient
public class InvoiceApp {
   public static void main(String[] args) {
      SpringApplication.run(InvoiceApp.class, args);
   }
}


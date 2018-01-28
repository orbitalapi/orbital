package io.vyne.demos.conversion.tradeThreshold;

import io.polymer.spring.EnablePolymer;
import io.polymer.spring.RemoteSchemaStoreType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@EnablePolymer(remoteSchemaStore = RemoteSchemaStoreType.HAZELCAST)
@EnableEurekaClient
@SpringBootApplication
public class TradeThresholdApp {

   public static void main(String[] args) {
      SpringApplication.run(TradeThresholdApp.class, args);
   }
}

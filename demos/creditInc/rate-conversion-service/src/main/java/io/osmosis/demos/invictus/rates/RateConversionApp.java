package io.osmosis.demos.invictus.rates;

import io.polymer.spring.EnablePolymer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnablePolymer(useRemoteSchemaStore = true)
@EnableEurekaClient
public class RateConversionApp {
   public static void main(String[] args) {
      SpringApplication.run(RateConversionApp.class, args);
   }
}

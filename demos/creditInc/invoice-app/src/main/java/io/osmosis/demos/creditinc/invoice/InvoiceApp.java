package io.osmosis.demos.creditinc.invoice;

import io.polymer.spring.EnablePolymer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnablePolymer
public class InvoiceApp {
   public static void main(String[] args) {
      SpringApplication.run(InvoiceApp.class, args);
   }
}


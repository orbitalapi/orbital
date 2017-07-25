package io.osmosis.demos.invictus.credit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * App which calculates the markup to
 * add to an invoice as a cost of purchasing from a retailer.
 */
@SpringBootApplication
public class CreditMarkupServiceApp {

   public static void main(String[] args) {
      SpringApplication.run(CreditMarkupServiceApp.class, args);
   }
}

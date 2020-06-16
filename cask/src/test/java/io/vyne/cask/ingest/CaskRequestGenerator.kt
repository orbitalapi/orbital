package io.vyne.cask.ingest

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.utils.log
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import org.springframework.web.reactive.socket.client.WebSocketClient
import reactor.core.publisher.Flux
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.*


/**
 * How to use this client
 * 1. Start vyne and schema file server
 * 2. Start cask
 * 3. select number of messages  per second
 * 4. Start this client
 * 5. Go to http://localhost:8800/actuator/prometheus and observe cask_ingestion_request_seconds metric
 */
class CaskRequestGenerator {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         generateRequests(20, "/cask/csv/hpc.orders.Order?debug=true&csvDelimiter=;&nullValue=NULL&nullValue=NULL", { hpcOrderCsv() })
         //start(20, "/cask/OrderWindowSummary", { generateCaskRequest() })
      }

      private fun generateRequests(noOfRequestsPerSecond: Int, uri: String, request: () -> String?) {
         val client: WebSocketClient = ReactorNettyWebSocketClient()
         client.execute(URI.create("ws://localhost:8800$uri")) { session ->
            val requestIntervalInMs = Duration.ofMillis(1000L / noOfRequestsPerSecond)
            val requestStream = Flux.interval(requestIntervalInMs)
               .map { request() }
               .doOnNext { log().info("${Instant.now()} sending request ${it}") }
               .map { session.textMessage(it) }
            session.send(requestStream).then()
         }.block()
      }

      private fun hpcOrderCsv(): String? {
         return """
securityClassification;entryType;entitySubmittingOrder;directElectronicAccess;clientIdentificationCode;investmentDecisionWithinFirm;executionWithinFirm;nonExecutingBroker;tradingCapacity;liquidityProvisionActivity;orderDateTime;validityPeriod;orderRestriction;validityPeriodAndTime;validityPeriodAndTime;priorityTimeStamp;prioritySize;sequenceNumber;segmentMicCode;orderBookCode;financialInstrumentIdentificationCode;dateOfReceipt;orderIdentificationCode;orderEventStatus;orderType;orderTypeClassification;limitPrice;additionalLimitPrice;stopPrice;peggedLimitPrice;tradeType;counterpartyLeiNonRts24Field;transactionPrice;priceCurrency;currencyOfLeg2;priceNotation;buySellIndicator;orderStatus;quantityNotation;quantityCurrency;initialQuantity;remainingQuantityIncludingHidden;displayedQuantity;tradedQuantity;minimumAcceptableQuantity;minimumExecutableSize;minimumExecutableSizeFirstExecutionOnly;passiveOnlyIndicator;passiveAggresiveIndicator;selfExecutionPrevention;strategyLinkedOrderIdentification;routingStrategy;tradingVenueTransactionIdentificationCode;tradingPhases;indicativeAuctionPrice;indicativeAuctionVolume;floatingReferenceIndexNonRts24Field;id;book;yield;leg1Currency;leg1DayCountMethodInd;leg1NotionalCurrencyCd;leg1NotionalValue;leg1OrigCurrNotionalAmount;leg1PaymentFrequency;leg1Rate;leg1RateSpread;leg1ResetFrequency1;leg2Currency;leg2DayCountMethodInd;leg2NotionalCurrencyCd;leg2NotionalValue2;leg2OrigCurrNotionalAmount;leg2PaymentFrequency2;leg2Rate;leg2RateSpread;leg2ResetFrequency2;leg2SwapRate
Conventional Bond;Opened;969500AMLHB21RACL168;1;1VUV7VQFKUOQSJ21A208;11111111;ELECTRONICPLATFORM;NULL;MTCH;NULL;2019-12-03 16:08:18.0960000;GTCV;NULL;NULL;2019-09-05 00:50:08.0000000;2019-12-03 16:08:18.0960000;25000000.0000000000;1;HPCO;NULL;NA;2019-12-03 16:08:18.0960000;18643_18644_2;NEWO;Limit;LMTO;15.4000000000;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;BAPO;BUY;NULL;NOML;EUR;25000000.0000000000;25000000.0000000000;25000000.0000000000;NULL;NULL;NULL;NULL;0;NULL;1;18643;NULL;NULL;NULL;NULL;NULL;NULL;10232;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL
         """.trimIndent()
      }

      private fun generateCaskRequest(): String? {
         val orders = generateOrders()
         val mapper = jacksonObjectMapper()
                  .registerModule(JavaTimeModule())
                  // enforcing property names starting with uppercase letter
                  .setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
          return mapper.writeValueAsString(orders)
      }

      private fun generateOrders(): List<OrderWindowSummary> {
         val random = Random()
         return (0..10).map {
            OrderWindowSummary(
               LocalDate.now().minusDays(1L * random.nextInt(10)),
               currencyPairs[random.nextInt(5)],
               1 + random.nextDouble(),
               1 + random.nextDouble(),
               1 + random.nextDouble(),
               1 + random.nextDouble()
            )
         }
      }

      val currencyPairs = listOf("GBPUSD", "PLNUSD", "EURUSD", "CHFUSD", "JPYUSD")

      data class OrderWindowSummary(
         val Date: LocalDate,
         val Symbol: String,
         val Open: Double,
         val Close: Double,
         val High: Double,
         val Low: Double
      )
   }
}

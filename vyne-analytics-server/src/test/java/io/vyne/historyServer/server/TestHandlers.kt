package io.vyne.historyServer.server

import io.vyne.models.Provided
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.json.Jackson
import io.vyne.query.QueryResponse
import io.vyne.query.history.LineageRecord
import io.vyne.query.history.QueryEndEvent
import io.vyne.query.history.QueryResultRow
import io.vyne.query.history.QuerySummary
import io.vyne.query.history.RemoteCallResponse
import io.vyne.query.history.VyneHistoryRecord
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.MessageMapping
import reactor.core.publisher.Flux
import java.time.Instant

private val logger = KotlinLogging.logger {}

open class TestHandler<T: VyneHistoryRecord>(private val messageCount: Int = 1, private val data: T) {
   @MessageMapping("historyRecords")
   fun queryEvents(): Flux<VyneHistoryRecord> {
      logger.info { "returning queryEvents" }
      return Flux.create { emitter ->
         (1..messageCount).forEach { _ -> emitter.next(data) }
      }
   }
}
class QuerySummaryTestClient(messageCount: Int = 1):
   TestHandler<QuerySummary>(messageCount, querySummary) {
   companion object {
      val querySummary = QuerySummary(
       queryId = "queryId",
         clientQueryId = "client query Id",
         taxiQl = null,
         queryJson = null,
         startTime = Instant.EPOCH,
         responseStatus =  QueryResponse.ResponseStatus.COMPLETED,
         endTime = Instant.EPOCH.plusMillis(1),
         recordCount = 1,
         errorMessage = null,
         id = null,
         anonymousTypesJson = null
      )
   }
}

class QueryResultRowEventHandler(messageCount: Int = 1):
TestHandler<QueryResultRow>(messageCount, queryResultRow) {
   companion object {
      val objectMapper = Jackson.defaultObjectMapper
      val results = """[
         |{ "firstName" : "Jimmy" , "lastName" : "Schmitts" , "age" : 50 },
         |{ "firstName" : "Peter" , "lastName" : "Papps" , "age" : 50 } ]""".trimMargin()
      val typedInstance = TypedInstance.from(
         SchemaHelper.schema.type("Person[]"), results, schema = SchemaHelper.schema, source = Provided
      ) as TypedCollection
      val queryResultRows = typedInstance.map { it.toTypeNamedInstance() }
      .map { QueryResultRow(queryId = "", json = objectMapper.writeValueAsString(it), valueHash = 123) }
      val queryResultRow = queryResultRows.first()
   }
}

class LineageRecordEventHandler(messageCount: Int = 1):
   TestHandler<LineageRecord>(messageCount, lineageRecord) {
      companion object {
         val largeString = (0 until 1000).joinToString(separator = "") { "1" }
         val lineageRecord = LineageRecord(
         "dataSourceId",
         "queryId",
         "foo.bar.Baz",
         largeString
         )
      }
   }

class RemoteCallResponseEventHandler(messageCount: Int = 1):
   TestHandler<RemoteCallResponse>(messageCount, remoteCallResponse){
   companion object {
      val remoteCallResponse = RemoteCallResponse(
         "response-id",
         "remote-call-id",
         "query-id",
         """{"smyrnaOrderId":"2020102801000000000000007754710F_1","entryType":"FILL","orderDateTime":1603871617.912185000,"orderDate":1603843200000,"venueOrderStatus":"FILL","cfiCode":"JFTXFP","identifierValue":"EZ91WZD7WC90","tempCfiCode":"JFTXFP","tempCfiCodeForIdentType1Char":"J","tempCfiCodeForIdentType2Char":"JF","tempCfiCodeForIdentType3Char":"JFT","tempCfiCodeForIdentTypeMid":"X","identifierType":"ICAPCCYPAIR","isin":"EZ91WZD7WC90","subSecurityType":"FWD.JPY.USD.1D","priceAmount":104.2,"stopPrice":null,"priceType":"BAPO","requestedQuantity":37000000000,"cumulativeQuantity":37000000000,"remainingQuantity":0,"displayedQuantity":0,"quantityNotation":"MONE","quantityCurrency":"JPY","unitMultiplier":1,"orderType":"Limit","buySellIndicator":"BUYI","orderValidityPeriod":"IOCV","exchange":"XOFF","sourceSystem":"XOFF","tempPayReceive":"JFTXFP-BUYI","leg1PayReceive":null,"leg2PayReceive":null,"tempLegRate":"JFTXFP-BUYI","leg1Rate":null,"leg2Rate":null,"trader":null,"cacibTraderBrokerLogin":null,"brokerVenue":"XOFF","underlyingIdentifierType":"ISIN","underlyingIdentifierValue":null,"tempLegs":"JFTXFP","leg1NotionalValue":null,"leg1OrigCurrNotionalAmount":null,"leg2NotionalValue":null,"leg2OrigCurrNotionalAmount":null,"leg2Currency":null,"method":"GUI","activityCategory":"Hedge","clientid":"SC0000041353","counterpartyLei":"21380076S228I25PD704","counterParty":"ICAP EUROPE LIMITED","cacibLei":"1VUV7VQFKUOQSJ21A208","tempTradeActivityType":"Central Limit Order Book","tradeActivityType":"OTH","brokerName":"icap","caskmessageid":"b79b7aa7-16a4-43e1-ba99-5e6759618691","cask_raw_id":"834e691f-fd77-4780-bf83-57868204eb2d"} """
      )
   }
}

class QueryEndEventEventHandler(messageCount: Int = 1):
   TestHandler<QueryEndEvent>(messageCount, queryEndEvent){

   companion object {
      val queryEndEvent = QueryEndEvent(
         queryId = "queryId",
         endTime = Instant.EPOCH,
         status = QueryResponse.ResponseStatus.COMPLETED,
         message = null,
         recordCount = 1
      )
   }
}
object SchemaHelper {
   private val providerAndSchema = io.vyne.spring.SimpleTaxiSchemaProvider.from(
      """
         model Person {
            firstName : String
            lastName : String
            age : Int
         }
      """
   )

   val schema = providerAndSchema.second

}

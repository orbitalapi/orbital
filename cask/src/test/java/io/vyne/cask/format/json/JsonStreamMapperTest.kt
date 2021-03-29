package io.vyne.cask.format.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.io.Resources
import io.vyne.VersionedTypeReference
import io.vyne.cask.MessageIds
import io.vyne.utils.Benchmark
import io.vyne.utils.log
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import reactor.core.publisher.Flux
import java.io.File

class JsonStreamMapperTest {
   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Test
   fun can_ingestAndMapToTypedInstance() {
      val schema = CoinbaseJsonOrderSchema.schemaV1
      val typeReference = "OrderWindowSummary"
      val versionedType = schema.versionedType(VersionedTypeReference.parse(typeReference))
      val resource = Resources.getResource("Coinbase_BTCUSD.json").toURI()

      // Ingest it a few times to get an average performance
      Benchmark.benchmark("can_ingestAndMapToTypedInstance") {
         val stream = JsonStreamSource(Flux.just(File(resource).inputStream()), versionedType, schema, MessageIds.uniqueId(), ObjectMapper())
         val noOfMappedRows = stream
            .stream
            .count()
            .block()

         log().info("Mapped ${noOfMappedRows} rows to typed instance")
      }
   }

   @Test
   fun complex() {
      val complexJson = """
         {
           "account": "HCFIX",
           "avgPx": 0.0,
           "clOrdID": "MUP7iJ/ofOOyKwWF+dfW4J:2",
           "cumQty": 0.0,
           "currency": "USD",
           "execID": "82724:2607704",
           "execInst": [
             "2",
             "WORK"
           ],
           "execTransType": [
             "0",
             "NEW"
           ],
           "iDSource": [
             "96",
             "TT_SECURITY_ID"
           ],
           "orderID": "ee7ce89f-88fb-4331-895b-5fe71716acc8",
           "orderQty": 1.0,
           "ordStatus": [
             "A",
             "PENDING_NEW"
           ],
           "ordType": [
             "2",
             "LIMIT"
           ],
           "price": 9976.0,
           "securityID": "5878123771958347261",
           "side": "1",
           "symbol": "GE",
           "timeInForce": [
             "0",
             "DAY"
           ],
           "transactTime": "20201104-13:23:47.529734",
           "openClose": [
             "O",
             "OPEN"
           ],
           "exDestination": "XCME",
           "securityDesc": "Eurodollar Futures",
           "execType": [
             "A",
             "PENDING_NEW"
           ],
           "leavesQty": 1.0,
           "securityType": [
             "FUT",
             "FUTURE"
           ],
           "secondaryOrderID": "821151879161",
           "maturityMonthYear": "202012",
           "maturityDay": 14,
           "securityExchange": "CME",
           "multiLegReportingType": [
             "1",
             "SINGLE_SECURITY"
           ],
           "noPartyIDs": [
             3,
             {
               "partyID": "0",
               "partyRole": [
                 "205",
                 "ACCOUNT_TYPE"
               ],
               "partyIDSource": [
                 "D",
                 "PROPRIETARY"
               ]
             },
             {
               "partyID": "123",
               "partyRole": [
                 "12",
                 "EXECUTING_TRADER"
               ],
               "partyRoleQualifier": [
                 "24",
                 "NATURAL_PERSON"
               ],
               "partyIDSource": [
                 "D",
                 "PROPRIETARY"
               ]
             },
             {
               "partyID": "HCFIX",
               "partyRole": [
                 "83",
                 "CLEARING_ACCOUNT"
               ],
               "partyIDSource": [
                 "D",
                 "PROPRIETARY"
               ]
             }
           ],
           "noSecurityAltID": [
             6,
             {
               "securityAltID": "GEZ0",
               "securityAltIDSource": [
                 "98",
                 "NAME"
               ]
             },
             {
               "securityAltID": "GE Dec20",
               "securityAltIDSource": [
                 "97",
                 "ALIAS"
               ]
             },
             {
               "securityAltID": "1EDZ0",
               "securityAltIDSource": [
                 "5",
                 "RIC_CODE"
               ]
             },
             {
               "securityAltID": "870833",
               "securityAltIDSource": [
                 "8",
                 "EXCHANGE_SECURITY_ID"
               ]
             },
             {
               "securityAltID": "EDZ0 Comdty",
               "securityAltIDSource": [
                 "A",
                 "BLOOMBERG_CODE"
               ],
               "bloombergSecurityExchange": "CME"
             },
             {
               "securityAltID": "BBG001BH7R55",
               "securityAltIDSource": [
                 "S",
                 "OPENFIGI_ID"
               ],
               "bloombergSecurityExchange": "CME"
             }
           ],
           "product": [
             "6",
             "GOVERNMENT"
           ],
           "cFICode": "FFDXSX",
           "secondaryClOrdID": "1604436169851",
           "secondaryExecID": "ee7ce89f-88fb-4331-895b-5fe71716acc8:2",
           "maturityDate": "20201214",
           "custOrderCapacity": [
             "4",
             "ALL_OTHER"
           ],
           "manualOrderIndicator": [
             "Y",
             "MANUAL"
           ],
           "custOrderHandlingInst": [
             "W",
             "DESK"
           ],
           "tTClOrdID": "2",
           "tTID": "harry.christodoulou@tradingtechnologies.com",
           "orderSource": [
             "2",
             "SOURCE_NTW"
           ],
           "textTT": "HCFIX",
           "timeReceivedFromExchange": "20201104-13:23:47.565384",
           "dropCopyOrder": [
             "Y",
             "YES"
           ],
           "uniqueExecID": "AaJCfIMUr6aBUoN18Ut9uV",
           "clearingAccountOverride": "HCFIX",
           "accountID": "2433",
           "userID": "1676",
           "deliveryTerm": [
             "M",
             "MONTH"
           ],
           "exchCred": "L18004",
           "tTCustomerName": "<DEFAULT>",
           "brokerID": "DTS",
           "companyID": "DTS",
           "contractYearMonth": "202012",
           "tTSyntheticType": 0
         }
      """.trimIndent()
   }
}

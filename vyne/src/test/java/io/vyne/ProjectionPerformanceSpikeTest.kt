package io.vyne

import com.winterbe.expekt.should
import io.vyne.cask.api.CsvIngestionParameters
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.csv.CsvImporterUtil
import io.vyne.models.json.addJsonModel
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.Benchmark
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.nio.file.Paths

@Ignore("This test requires access to client specific content, which is not checked in.  Keeping the test here, as its useful for spiking perf improvements.")
class ProjectionPerformanceSpikeTest {
   val twoItems = """
      [
  {
    "icapOrderId": "102J3SQ7BOBUY",
    "entryType": "NEWO",
    "orderDateTime": 1599579067198,
    "orderDate": 1599523200000,
    "venueOrderStatus": "NEWO",
    "cfiCode": "SRCCSP",
    "identifierValue": "EZVPTCCDHVJ0",
    "identifierType": "ISIN",
    "isin": "EZVPTCCDHVJ0",
    "subSecurityType": null,
    "priceAmount": 20.600000000000000,
    "stopPrice": null,
    "priceType": "PERC",
    "quantity": 100800000.000000000000000,
    "quantityNotation": "MONE",
    "quantityRequired": 100800000.000000000000000,
    "qtyHit": 0E-15,
    "unitMultiplier": 1.000000000000000,
    "tradedQuantity": 0E-15,
    "quantityCurrency": "EUR",
    "orderType": "Limit",
    "buySellIndicator": "BUYI",
    "orderValidityPeriod": "GTCV",
    "exchange": "XOFF",
    "sourceSystem": "XOFF",
    "tempPayReceive": "SRCCSP-BUYI",
    "leg1PayReceive": "Pay",
    "leg2PayReceive": "Receive",
    "tempLegRate": "SRCCSP-BUYI",
    "leg1Rate": 20.600000000000000,
    "leg2Rate": null,
    "trader": null,
    "brokerVenue": "XOFF",
    "underlyingIdentifierType": "ISIN",
    "underlyingIdentifierValue": null,
    "tempLegs": "SRCCSP",
    "leg1NotionalValue": 100800000.000000000000000,
    "leg1OrigCurrNotionalAmount": 100800000.000000000000000,
    "leg2NotionalValue": 100800000.000000000000000,
    "leg2OrigCurrNotionalAmount": 100800000.000000000000000,
    "leg2Currency": null,
    "method": "Voice",
    "activityCategory": "Hedge",
    "clientid": "SC0000041353",
    "counterpartyLei": "21380076S228I25PD704",
    "counterParty": "ICAP EUROPE LIMITED",
    "caskmessageid": "6e5657ee-34aa-4bed-9dca-21fbc03abdfb"
  },
  {
    "icapOrderId": "2020090800990000000000007595276U_1",
    "entryType": "NEWO",
    "orderDateTime": 1599572907819,
    "orderDate": 1599523200000,
    "venueOrderStatus": "NEWO",
    "cfiCode": "DBFTFB",
    "identifierValue": "DE0001102507",
    "identifierType": "ISIN",
    "isin": "DE0001102507",
    "subSecurityType": "DBR 0% 08/15/30",
    "priceAmount": 105.160000000000000,
    "stopPrice": null,
    "priceType": "MONE",
    "quantity": 2200000.000000000000000,
    "quantityNotation": "MONE",
    "quantityRequired": 2200000.000000000000000,
    "qtyHit": 0E-15,
    "unitMultiplier": 1.000000000000000,
    "tradedQuantity": 0E-15,
    "quantityCurrency": "EUR",
    "orderType": "Limit",
    "buySellIndicator": "BUYI",
    "orderValidityPeriod": "IOCV",
    "exchange": "IOFI",
    "sourceSystem": "IOFI",
    "tempPayReceive": "DBFTFB-BUYI",
    "leg1PayReceive": null,
    "leg2PayReceive": null,
    "tempLegRate": "DBFTFB-BUYI",
    "leg1Rate": null,
    "leg2Rate": null,
    "trader": "100297",
    "brokerVenue": "IOFI",
    "underlyingIdentifierType": "ISIN",
    "underlyingIdentifierValue": null,
    "tempLegs": "DBFTFB",
    "leg1NotionalValue": null,
    "leg1OrigCurrNotionalAmount": null,
    "leg2NotionalValue": null,
    "leg2OrigCurrNotionalAmount": null,
    "leg2Currency": null,
    "method": "Voice",
    "activityCategory": "Hedge",
    "clientid": "SC0000041353",
    "counterpartyLei": "21380076S228I25PD704",
    "counterParty": "ICAP EUROPE LIMITED",
    "caskmessageid": "6e5657ee-34aa-4bed-9dca-21fbc03abdfb"
  }]
   """.trimIndent()
   val jsonPath = "/home/serhat/Documents/icap_findAll_cask.json"
   val returnJson = File(jsonPath).readText()
   val schemaPath = "/home/serhat/Downloads/vyneinternalservererror/src"
   val sourceRoot = Paths.get(schemaPath)
   val sources = sourceRoot.toFile().walkBottomUp()
      .filter { it.extension == "taxi" }
      .map {
         val pathRelativeToSourceRoot = sourceRoot.relativize(it.toPath()).toString()
         VersionedSource(pathRelativeToSourceRoot, "0.1.0", it.readText())
      }
      .toList() + listOf(VersionedSource("cask.taxi", "0.1.0", """
         @Datasource
         service IcapOrderService {
           operation findAll(): icap.orders.Order[]
         }
      """.trimIndent()))
   val schema = TaxiSchema.from(sources)
   private val schemaStr = """
      namespace broker.orders

      enum QuantityType {
        UNIT,
        NOML,
        MONE,
        UNKNOWN
      }

       enum OrderStatus {
      New,
      PartiallyFilled,
      Filled,
      DoneForDay,
      Canceled,
      PendingCancel,
      Stopped,
      Suspended,
      PendingNew,
      Calculated,
      Expired,
      AcceptedForBidding,
      PendingReplaced,
      Replaced,
      Deleted,
      PlatformSpecified,
      ChangeByMember,
      ChangeByMarketOps,
      Rejected
   }

   enum VenueOrderStatus {
      New,
      PartiallyFilled,
      Filled,
      DoneForDay,
      Canceled,
      PendingCancel,
      Stopped,
      Rejected,
      Suspended,
      PendingNew,
      Calculated,
      Expired,
      AcceptedForBidding,
      PendingReplaced,
      Replaced,
      Deleted,
      PlatformSpecified,
      ChangeByMember,
      ChangeByMarketOps
   }

   enum OrderBankDirection {
       BankBuys,
       BankSell
   }

   enum Leg1BankPayReceive {
       Undef,
       Pay,
       Receive

   }

   enum Leg2BankPayReceive {
       Undef,
       Pay,
       Receive
   }

   enum OrderType {
      Simple,
      Market,
      Limit,
      Stop,
      StopLimit,
      Iceberg,
      GoodTillBettered
   }

   enum PriceType {
       Percentage,
       PerUnit,
       FixedAmount,
       Discount,
       Premium,
       Spread,
       TEDPrice,
       TEDYield,
       Yield,
       FixedCabinetTradePrice,
       VariableCabinetTradePrice,
       Basis,
       NormalRateRepresentation,
       InverseRateRepresentation,
       Unknown
   }

      enum BrokerQuantityNotation {
      	MONE("MONEY") synonym of QuantityType.MONE,
      	NOML("NOML") synonym of QuantityType.NOML,
      	UNIT("UNIT") synonym of QuantityType.UNIT,
      	UNKNOWN("UNKNOWN") synonym of QuantityType.UNKNOWN
      }

      enum BrokerOrderStatus {
      	New("NEWO") synonym of OrderStatus.New,
      	Replaced("REME") synonym of OrderStatus.Replaced,
      	CanceledByMarketEvents("CAME") synonym of OrderStatus.Canceled,
      	CanceledByMarketOperation("CAMO") synonym of OrderStatus.Canceled,
      	PartiallyFilled("PARF") synonym of OrderStatus.PartiallyFilled,
      	Filled("FILL") synonym of OrderStatus.Filled,
      	StatusChangeCHME("CHME") synonym of OrderStatus.ChangeByMember,
      	StatusChangeCHMO("CHMO") synonym of OrderStatus.ChangeByMarketOps,
      	Rejected("REMO") synonym of OrderStatus.Rejected
      }

      enum BrokerVenueOrderStatus {
      	New("NEWO") synonym of VenueOrderStatus.New,
      	Replaced("REME") synonym of VenueOrderStatus.Replaced,
      	CanceledByMarketEvents("CAME") synonym of VenueOrderStatus.Canceled,
      	CanceledByMarketOperation("CAMO") synonym of VenueOrderStatus.Canceled,
      	PartiallyFilled("PARF") synonym of VenueOrderStatus.PartiallyFilled,
      	Filled("FILL") synonym of VenueOrderStatus.Filled,
      	StatusChangeCHME("CHME") synonym of VenueOrderStatus.ChangeByMember,
      	StatusChangeCHMO("CHMO") synonym of VenueOrderStatus.ChangeByMarketOps,
      	Rejected("REMO") synonym of VenueOrderStatus.Rejected
      }

      enum BrokerOrderBankDirection {
      	BUY("BUYI") synonym of OrderBankDirection.BankBuys,
      	SELL("SELL") synonym of OrderBankDirection.BankSell
      }

      enum BrokerLeg1PayReceive {
      	Pay("BUYI") synonym of Leg1BankPayReceive.Pay,
      	Receive("SELL") synonym of Leg1BankPayReceive.Receive
      }

      enum BrokerLeg2PayReceive {
      	Receive("BUYI") synonym of Leg2BankPayReceive.Receive,
      	Pay("SELL") synonym of Leg2BankPayReceive.Pay
      }

      enum BrokerOrderType {
      	Limit("LMTO") synonym of OrderType.Limit,
      	Stop("STOP") synonym of OrderType.Stop
      }

      enum BrokerPriceType {
      	Basis("BAPO") synonym of PriceType.Basis,
      	FixedAmount("MONE") synonym of PriceType.FixedAmount,
      	Percentage("PERC") synonym of PriceType.Percentage
      }

      enum CurrencyCode {
          [[United Arab Emirates Dirham]]
          AED,
          [[Afghanistan Afghani]]
          AFN,
          [[Albania Lek]]
          ALL,
          [[Armenia Dram]]
          AMD,
          [[Netherlands Antilles Guilder]]
          ANG,
          [[Angola Kwanza]]
          AOA,
          [[Argentina Peso]]
          ARS,
          [[Australia Dollar]]
          AUD,
          [[Aruba Guilder]]
          AWG,
          [[Azerbaijan Manat]]
          AZN,
          [[Bosnia and Herzegovina Convertible Mark]]
          BAM,
          [[Barbados Dollar]]
          BBD,
          [[Bangladesh Taka]]
          BDT,
          [[Bulgaria Lev]]
          BGN,
          [[Bahrain Dinar]]
          BHD,
          [[Burundi Franc]]
          BIF,
          [[Bermuda Dollar]]
          BMD,
          [[Brunei Darussalam Dollar]]
          BND,
          [[Bolivia Bolíviano]]
          BOB,
          [[Brazil Real]]
          BRL,
          [[Bahamas Dollar]]
          BSD,
          [[Bhutan Ngultrum]]
          BTN,
          [[Botswana Pula]]
          BWP,
          [[Belarus Ruble]]
          BYN,
          [[Belize Dollar]]
          BZD,
          [[Canada Dollar]]
          CAD,
          [[Congo/Kinshasa Franc]]
          CDF,
          [[Switzerland Franc]]
          CHF,
          [[Chile Peso]]
          CLP,
          [[China Yuan Renminbi]]
          CNY,
          [[Colombia Peso]]
          COP,
          [[Costa Rica Colon]]
          CRC,
          [[Cuba Convertible Peso]]
          CUC,
          [[Cuba Peso]]
          CUP,
          [[Cape Verde Escudo]]
          CVE,
          [[Czech Republic Koruna]]
          CZK,
          [[Djibouti Franc]]
          DJF,
          [[Denmark Krone]]
          DKK,
          [[Dominican Republic Peso]]
          DOP,
          [[Algeria Dinar]]
          DZD,
          [[Egypt Pound]]
          EGP,
          [[Eritrea Nakfa]]
          ERN,
          [[Ethiopia Birr]]
          ETB,
          [[Euro Member Countries]]
          EUR,
          [[Fiji Dollar]]
          FJD,
          [[Falkland Islands (Malvinas) Pound]]
          FKP,
          [[United Kingdom Pound]]
          GBP,
          [[Georgia Lari]]
          GEL,
          [[Guernsey Pound]]
          GGP,
          [[Ghana Cedi]]
          GHS,
          [[Gibraltar Pound]]
          GIP,
          [[Gambia Dalasi]]
          GMD,
          [[Guinea Franc]]
          GNF,
          [[Guatemala Quetzal]]
          GTQ,
          [[Guyana Dollar]]
          GYD,
          [[Hong Kong Dollar]]
          HKD,
          [[Honduras Lempira]]
          HNL,
          [[Croatia Kuna]]
          HRK,
          [[Haiti Gourde]]
          HTG,
          [[Hungary Forint]]
          HUF,
          [[Indonesia Rupiah]]
          IDR,
          [[Israel Shekel]]
          ILS,
          [[Isle of Man Pound]]
          IMP,
          [[India Rupee]]
          INR,
          [[Iraq Dinar]]
          IQD,
          [[Iran Rial]]
          IRR,
          [[Iceland Krona]]
          ISK,
          [[Jersey Pound]]
          JEP,
          [[Jamaica Dollar]]
          JMD,
          [[Jordan Dinar]]
          JOD,
          [[Japan Yen]]
          JPY,
          [[Kenya Shilling]]
          KES,
          [[Kyrgyzstan Som]]
          KGS,
          [[Cambodia Riel]]
          KHR,
          [[Comorian Franc]]
          KMF,
          [[Korea (North) Won]]
          KPW,
          [[Korea (South) Won]]
          KRW,
          [[Kuwait Dinar]]
          KWD,
          [[Cayman Islands Dollar]]
          KYD,
          [[Kazakhstan Tenge]]
          KZT,
          [[Laos Kip]]
          LAK,
          [[Lebanon Pound]]
          LBP,
          [[Sri Lanka Rupee]]
          LKR,
          [[Liberia Dollar]]
          LRD,
          [[Lesotho Loti]]
          LSL,
          [[Libya Dinar]]
          LYD,
          [[Morocco Dirham]]
          MAD,
          [[Moldova Leu]]
          MDL,
          [[Madagascar Ariary]]
          MGA,
          [[Macedonia Denar]]
          MKD,
          [[Myanmar (Burma) Kyat]]
          MMK,
          [[Mongolia Tughrik]]
          MNT,
          [[Macau Pataca]]
          MOP,
          [[Mauritania Ouguiya]]
          MRU,
          [[Mauritius Rupee]]
          MUR,
          [[Maldives (Maldive Islands) Rufiyaa]]
          MVR,
          [[Malawi Kwacha]]
          MWK,
          [[Mexico Peso]]
          MXN,
          [[Malaysia Ringgit]]
          MYR,
          [[Mozambique Metical]]
          MZN,
          [[Namibia Dollar]]
          NAD,
          [[Nigeria Naira]]
          NGN,
          [[Nicaragua Cordoba]]
          NIO,
          [[Norway Krone]]
          NOK,
          [[Nepal Rupee]]
          NPR,
          [[New Zealand Dollar]]
          NZD,
          [[Oman Rial]]
          OMR,
          [[Panama Balboa]]
          PAB,
          [[Peru Sol]]
          PEN,
          [[Papua New Guinea Kina]]
          PGK,
          [[Philippines Peso]]
          PHP,
          [[Pakistan Rupee]]
          PKR,
          [[Poland Zloty]]
          PLN,
          [[Paraguay Guarani]]
          PYG,
          [[Qatar Riyal]]
          QAR,
          [[Romania Leu]]
          RON,
          [[Serbia Dinar]]
          RSD,
          [[Russia Ruble]]
          RUB,
          [[Rwanda Franc]]
          RWF,
          [[Saudi Arabia Riyal]]
          SAR,
          [[Solomon Islands Dollar]]
          SBD,
          [[Seychelles Rupee]]
          SCR,
          [[Sudan Pound]]
          SDG,
          [[Sweden Krona]]
          SEK,
          [[Singapore Dollar]]
          SGD,
          [[Saint Helena Pound]]
          SHP,
          [[Sierra Leone Leone]]
          SLL,
          [[Somalia Shilling]]
          SOS,
          [[Seborga Luigino]]
          SPL,
          [[Suriname Dollar]]
          SRD,
          [[São Tomé and Príncipe Dobra]]
          STN,
          [[El Salvador Colon]]
          SVC,
          [[Syria Pound]]
          SYP,
          [[eSwatini Lilangeni]]
          SZL,
          [[Thailand Baht]]
          THB,
          [[Tajikistan Somoni]]
          TJS,
          [[Turkmenistan Manat]]
          TMT,
          [[Tunisia Dinar]]
          TND,
          [[Tonga Pa anga]]
          TOP,
          [[Turkey Lira]]
          TRY,
          [[Trinidad and Tobago Dollar]]
          TTD,
          [[Tuvalu Dollar]]
          TVD,
          [[Taiwan New Dollar]]
          TWD,
          [[Tanzania Shilling]]
          TZS,
          [[Ukraine Hryvnia]]
          UAH,
          [[Uganda Shilling]]
          UGX,
          [[United States Dollar]]
          USD,
          [[Uruguay Peso]]
          UYU,
          [[Uzbekistan Som]]
          UZS,
          [[Venezuela Bolívar]]
          VEF,
          [[Viet Nam Dong]]
          VND,
          [[Vanuatu Vatu]]
          VUV,
          [[Samoa Tala]]
          WST,
          [[Communauté Financière Africaine (BEAC) CFA Franc BEAC]]
          XAF,
          [[East Caribbean Dollar]]
          XCD,
          [[International Monetary Fund (IMF) Special Drawing Rights]]
          XDR,
          [[Communauté Financière Africaine (BCEAO) Franc]]
          XOF,
          [[Comptoirs Français du Pacifique (CFP) Franc]]
          XPF,
          [[Yemen Rial]]
          YER,
          [[South Africa Rand]]
          ZAR,
          [[Zambia Kwacha]]
          ZMW,
          [[Zimbabwe Dollar]]
          ZWD,
          [[Chinese Yuan Renminbi]]
          CNH,
      	NA("")
      }

      enum TimeInForce {
      //some values
      [[
      Good for Day
      ]]
      Day,
      [[
      Good till Cancel
      ]]
      GTC,
      [[
      At the Opening
      ]]
      OPG,
      [[
      Immediate or Cancel
      ]]
      IOC,
      [[
      Fill Or Kill
      ]]
      FOK,
      [[
      Good till Crossing
      ]]
      GTX,
      [[
      Good till Date
      ]]
      GTD,

      [[
      At the Close
      ]]
      ATC,

      [[
      Fill and Store, operates in a similar capacity as a limit order. A FaS order, once submitted, will remain on the CLOB until completely filled and submitted for clearance and settlement.

      If a FaS is partially filled, the unfilled portion of the order is automatically converted to a new order for the unfilled size and added to the order book, subject to the price and time priority of the matching engine.
      ]]
      FAS,
      [[Good til time]]
      GTT,
      [[
      Non-Fix Defined, platform-specified
      ]]

      PlatformSpecified
   }

      enum ValidityPeriod {
      [[
      Good-For-Day: the order expires at the end of the trading day on which it was entered in the order book
      ]]

      DAVY synonym of TimeInForce.Day,

      [[
      Good-Till-Cancelled: the order will remain active in the order book and be executable until it is actually cancelled.
      ]]

      GTCV synonym of TimeInForce.GTC,

      [[
      Good-Till-Time: the order expires at the latest at a pre-determined time within the current trading session.
      ]]
      GTTV synonym of TimeInForce.GTT,
      [[
      Good-Till-Date: the order expires at the end of a specified date.
      ]]
      GDTV synonym of TimeInForce.GTD,

      [[
      Good-Till-Specified Date and Time: the order expires at a specified date and time.
      ]]
      GTSV synonym of TimeInForce.GTT,

      [[
      Good After Time: the order is only active after a pre-determined time within the current trading session
      ]]
      GATV,

      [[
      Good After Date: the order is only active from the beginning of a pre-determined date.
      ]]
      GADV,

      [[
      Good After Specified Date and Time: the order is only active from a pre-determined time on a pre-determined date.
      ]]
      GASV,

      [[
      Immediate-Or-Cancel: an order which is executed upon its entering into the order book (for the quantity that can be executed) and which does not remain in the order
      book for the remaining quantity (if any) that has not been executed.
      ]]
      IOCV synonym of TimeInForce.IOC,

      [[
      Fill-Or-Kill: an order which is executed upon its entering into the order book providedthat it can be fully filled:in the event the order can only be partially executed, then it is automatically rejected and cannot therefore be executed

      ]]
      FOKV synonym of TimeInForce.FOK

   }

   enum Leg2Currency {

       [[United Arab Emirates Dirham]]
       AED,
       [[Afghanistan Afghani]]
       AFN,
       [[Albania Lek]]
       ALL,
       [[Armenia Dram]]
       AMD,
       [[Netherlands Antilles Guilder]]
       ANG,
       [[Angola Kwanza]]
       AOA,
       [[Argentina Peso]]
       ARS,
       [[Australia Dollar]]
       AUD,
       [[Aruba Guilder]]
       AWG,
       [[Azerbaijan Manat]]
       AZN,
       [[Bosnia and Herzegovina Convertible Mark]]
       BAM,
       [[Barbados Dollar]]
       BBD,
       [[Bangladesh Taka]]
       BDT,
       [[Bulgaria Lev]]
       BGN,
       [[Bahrain Dinar]]
       BHD,
       [[Burundi Franc]]
       BIF,
       [[Bermuda Dollar]]
       BMD,
       [[Brunei Darussalam Dollar]]
       BND,
       [[Bolivia Bolíviano]]
       BOB,
       [[Brazil Real]]
       BRL,
       [[Bahamas Dollar]]
       BSD,
       [[Bhutan Ngultrum]]
       BTN,
       [[Botswana Pula]]
       BWP,
       [[Belarus Ruble]]
       BYN,
       [[Belize Dollar]]
       BZD,
       [[Canada Dollar]]
       CAD,
       [[Congo/Kinshasa Franc]]
       CDF,
       [[Switzerland Franc]]
       CHF,
       [[Chile Peso]]
       CLP,
       [[China Yuan Renminbi]]
       CNY,
       [[Colombia Peso]]
       COP,
       [[Costa Rica Colon]]
       CRC,
       [[Cuba Convertible Peso]]
       CUC,
       [[Cuba Peso]]
       CUP,
       [[Cape Verde Escudo]]
       CVE,
       [[Czech Republic Koruna]]
       CZK,
       [[Djibouti Franc]]
       DJF,
       [[Denmark Krone]]
       DKK,
       [[Dominican Republic Peso]]
       DOP,
       [[Algeria Dinar]]
       DZD,
       [[Egypt Pound]]
       EGP,
       [[Eritrea Nakfa]]
       ERN,
       [[Ethiopia Birr]]
       ETB,
       [[Euro Member Countries]]
       EUR,
       [[Fiji Dollar]]
       FJD,
       [[Falkland Islands (Malvinas) Pound]]
       FKP,
       [[United Kingdom Pound]]
       GBP,
       [[Georgia Lari]]
       GEL,
       [[Guernsey Pound]]
       GGP,
       [[Ghana Cedi]]
       GHS,
       [[Gibraltar Pound]]
       GIP,
       [[Gambia Dalasi]]
       GMD,
       [[Guinea Franc]]
       GNF,
       [[Guatemala Quetzal]]
       GTQ,
       [[Guyana Dollar]]
       GYD,
       [[Hong Kong Dollar]]
       HKD,
       [[Honduras Lempira]]
       HNL,
       [[Croatia Kuna]]
       HRK,
       [[Haiti Gourde]]
       HTG,
       [[Hungary Forint]]
       HUF,
       [[Indonesia Rupiah]]
       IDR,
       [[Israel Shekel]]
       ILS,
       [[Isle of Man Pound]]
       IMP,
       [[India Rupee]]
       INR,
       [[Iraq Dinar]]
       IQD,
       [[Iran Rial]]
       IRR,
       [[Iceland Krona]]
       ISK,
       [[Jersey Pound]]
       JEP,
       [[Jamaica Dollar]]
       JMD,
       [[Jordan Dinar]]
       JOD,
       [[Japan Yen]]
       JPY,
       [[Kenya Shilling]]
       KES,
       [[Kyrgyzstan Som]]
       KGS,
       [[Cambodia Riel]]
       KHR,
       [[Comorian Franc]]
       KMF,
       [[Korea (North) Won]]
       KPW,
       [[Korea (South) Won]]
       KRW,
       [[Kuwait Dinar]]
       KWD,
       [[Cayman Islands Dollar]]
       KYD,
       [[Kazakhstan Tenge]]
       KZT,
       [[Laos Kip]]
       LAK,
       [[Lebanon Pound]]
       LBP,
       [[Sri Lanka Rupee]]
       LKR,
       [[Liberia Dollar]]
       LRD,
       [[Lesotho Loti]]
       LSL,
       [[Libya Dinar]]
       LYD,
       [[Morocco Dirham]]
       MAD,
       [[Moldova Leu]]
       MDL,
       [[Madagascar Ariary]]
       MGA,
       [[Macedonia Denar]]
       MKD,
       [[Myanmar (Burma) Kyat]]
       MMK,
       [[Mongolia Tughrik]]
       MNT,
       [[Macau Pataca]]
       MOP,
       [[Mauritania Ouguiya]]
       MRU,
       [[Mauritius Rupee]]
       MUR,
       [[Maldives (Maldive Islands) Rufiyaa]]
       MVR,
       [[Malawi Kwacha]]
       MWK,
       [[Mexico Peso]]
       MXN,
       [[Malaysia Ringgit]]
       MYR,
       [[Mozambique Metical]]
       MZN,
       [[Namibia Dollar]]
       NAD,
       [[Nigeria Naira]]
       NGN,
       [[Nicaragua Cordoba]]
       NIO,
       [[Norway Krone]]
       NOK,
       [[Nepal Rupee]]
       NPR,
       [[New Zealand Dollar]]
       NZD,
       [[Oman Rial]]
       OMR,
       [[Panama Balboa]]
       PAB,
       [[Peru Sol]]
       PEN,
       [[Papua New Guinea Kina]]
       PGK,
       [[Philippines Peso]]
       PHP,
       [[Pakistan Rupee]]
       PKR,
       [[Poland Zloty]]
       PLN,
       [[Paraguay Guarani]]
       PYG,
       [[Qatar Riyal]]
       QAR,
       [[Romania Leu]]
       RON,
       [[Serbia Dinar]]
       RSD,
       [[Russia Ruble]]
       RUB,
       [[Rwanda Franc]]
       RWF,
       [[Saudi Arabia Riyal]]
       SAR,
       [[Solomon Islands Dollar]]
       SBD,
       [[Seychelles Rupee]]
       SCR,
       [[Sudan Pound]]
       SDG,
       [[Sweden Krona]]
       SEK,
       [[Singapore Dollar]]
       SGD,
       [[Saint Helena Pound]]
       SHP,
       [[Sierra Leone Leone]]
       SLL,
       [[Somalia Shilling]]
       SOS,
       [[Seborga Luigino]]
       SPL,
       [[Suriname Dollar]]
       SRD,
       [[São Tomé and Príncipe Dobra]]
       STN,
       [[El Salvador Colon]]
       SVC,
       [[Syria Pound]]
       SYP,
       [[eSwatini Lilangeni]]
       SZL,
       [[Thailand Baht]]
       THB,
       [[Tajikistan Somoni]]
       TJS,
       [[Turkmenistan Manat]]
       TMT,
       [[Tunisia Dinar]]
       TND,
       [[Tonga Pa anga]]
       TOP,
       [[Turkey Lira]]
       TRY,
       [[Trinidad and Tobago Dollar]]
       TTD,
       [[Tuvalu Dollar]]
       TVD,
       [[Taiwan New Dollar]]
       TWD,
       [[Tanzania Shilling]]
       TZS,
       [[Ukraine Hryvnia]]
       UAH,
       [[Uganda Shilling]]
       UGX,
       [[United States Dollar]]
       USD,
       [[Uruguay Peso]]
       UYU,
       [[Uzbekistan Som]]
       UZS,
       [[Venezuela Bolívar]]
       VEF,
       [[Viet Nam Dong]]
       VND,
       [[Vanuatu Vatu]]
       VUV,
       [[Samoa Tala]]
       WST,
       [[Communauté Financière Africaine (BEAC) CFA Franc BEAC]]
       XAF,
       [[East Caribbean Dollar]]
       XCD,
       [[International Monetary Fund (IMF) Special Drawing Rights]]
       XDR,
       [[Communauté Financière Africaine (BCEAO) Franc]]
       XOF,
       [[Comptoirs Français du Pacifique (CFP) Franc]]
       XPF,
       [[Yemen Rial]]
       YER,
       [[South Africa Rand]]
       ZAR,
       [[Zambia Kwacha]]
       ZMW,
       [[Zimbabwe Dollar]]
       ZWD
   }

    enum OrderMethod {
      ELECTRONIC("Electronic"),
      VOICE("Voice")
   }

     enum OrderActivityCategory {
      Hedge,
      Client
   }

   enum TradeFillStatus {

      [[Outstanding order with no executions ]]
      New,
      [[Outstanding order with executions and remaining quantity ]]
      PartiallyFilled,
      [[Order completely filled, no remaining quantity ]]
      Filled,
      [[Order not, or partially, filled. No further executions forthcoming for the trading day]]
      DoneForDay,
      [[Canceled order with or without executions ]]
      Canceled,
      [[Order with an Order Cancel Request pending, used to confirm receipt of an Order Cancel Request.]]
      PendingCancel,
      [[
      A trade is guaranteed for the order, usually at a stated price or better, but has not yet occurred.
      ]]
      Stopped,
      [[Order has been rejected by sell-side (broker, exchange, ECN). NOTE: An order can be rejected subsequent to order acknowledgment, i.e. an order can pass from New to Rejected status. ]]
      Rejected,
      [[
      The order is not eligible for trading. This usually happens as a result of a verbal or otherwise out of band request to suspend the order,
      or because the order was submitted, or modified by a subsequent message
      ]]
      Suspended,
      [[Order has been received by the sell-side (broker, exchange, ECN) system but not yet accepted for execution.]]
      PendingNew,
      [[Order has been completed for the day (either filled or done for day)]]
      Calculated,
      [[
      Order has been canceled in broker's system due to time in force instructions.
      The only exceptions are Fill or Kill and Immediate or Cancel orders that have Canceled as terminal order state
      ]]
      Expired,
      [[Order has been received and is being evaluated for pricing.]]
      AcceptedForBidding,
      [[Order with an Order Cancel-Replace Request pending. Used to confirm receipt of an Order Cancel/Replace Request]]
      PendingReplaced,
      [[
      Deprecated. Kept for completeness.  Order Replace Request has been applied to order.
      ]]
      Replaced,

      [[Record Removed from Platform as part of process ]]
      Deleted
   }

   enum BankAssetClass {
      Bond,
      Credit,
      CrossAsset,
      Equity,
      Fx,
      Rates,
      Inflation,
      InterestRate,
      PreciousMetal,
      Security
   }

   enum CountryCode {
   AFG,
   ALB,
   DZA,
   ASM,
   AND,
   AGO,
   AIA,
   ATA,
   ATG,
   ARG,
   ARM,
   ABW,
   AUS,
   AUT,
   AZE,
   BHS,
   BHR,
   BGD,
   BRB,
   BLR,
   BEL,
   BLZ,
   BEN,
   BMU,
   BTN,
   BOL,
   BES,
   BIH,
   BWA,
   BVT,
   BRA,
   IOT,
   BRN,
   BGR,
   BFA,
   BDI,
   CPV,
   KHM,
   CMR,
   CAN,
   CYM,
   CAF,
   TCD,
   CHL,
   CHN,
   CXR,
   CCK,
   COL,
   COM,
   COD,
   COG,
   COK,
   CRI,
   HRV,
   CUB,
   CUW,
   CYP,
   CZE,
   CIV,
   DNK,
   DJI,
   DMA,
   DOM,
   ECU,
   EGY,
   SLV,
   GNQ,
   ERI,
   EST,
   SWZ,
   ETH,
   FLK,
   FRO,
   FJI,
   FIN,
   FRA,
   GUF,
   PYF,
   ATF,
   GAB,
   GMB,
   GEO,
   DEU,
   GHA,
   GIB,
   GRC,
   GRL,
   GRD,
   GLP,
   GUM,
   GTM,
   GGY,
   GIN,
   GNB,
   GUY,
   HTI,
   HMD,
   VAT,
   HND,
   HKG,
   HUN,
   ISL,
   IND,
   IDN,
   IRN,
   IRQ,
   IRL,
   IMN,
   ISR,
   ITA,
   JAM,
   JPN,
   [[Jersey]]
   JEY,
   [[Jordan]]
   JOR,
   [[Kazakhstan]]
   KAZ,
   [[Kenya]]
   KEN,
   [[Kiribati]]
   KIR,
   [[Korea (the Democratic Peoples Republic of)]]
   PRK,
   [[Korea (the Republic of)]]
   KOR,
   [[Kuwait]]
   KWT,
   [[Kyrgyzstan]]
   KGZ,
   [[Lao Peoples Democratic Republic (the)]]
   LAO,
   [[Latvia]]
   LVA,
   [[Lebanon]]
   LBN,
   [[Lesotho]]
   LSO,
   [[Liberia]]
   LBR,
   [[Libya]]
   LBY,
   [[Liechtenstein]]
   LIE,
   [[Lithuania]]
   LTU,
   [[Luxembourg]]
   LUX,
   [[Macao]]
   MAC,
   [[Madagascar]]
   MDG,
   [[Malawi]]
   MWI,
   [[Malaysia]]
   MYS,
   [[Maldives]]
   MDV,
   [[Mali]]
   MLI,
   [[Malta]]
   MLT,
   [[Marshall Islands (the)]]
   MHL,
   [[Martinique]]
   MTQ,
   [[Mauritania]]
   MRT,
   [[Mauritius]]
   MUS,
   [[Mayotte]]
   MYT,
   [[Mexico]]
   MEX,
   [[Micronesia (Federated States of)]]
   FSM,
   [[Moldova (the Republic of)]]
   MDA,
   [[Monaco]]
   MCO,
   [[Mongolia]]
   MNG,
   [[Montenegro]]
   MNE,
   [[Montserrat]]
   MSR,
   [[Morocco]]
   MAR,
   [[Mozambique]]
   MOZ,
   [[Myanmar]]
   MMR,
   [[Namibia]]
   NAM,
   [[Nauru]]
   NRU,
   [[Nepal]]
   NPL,
   [[Netherlands (the)]]
   NLD,
   [[New Caledonia]]
   NCL,
   [[New Zealand]]
   NZL,
   [[Nicaragua]]
   NIC,
   [[Niger (the)]]
   NER,
   [[Nigeria]]
   NGA,
   [[Niue]]
   NIU,
   [[Norfolk Island]]
   NFK,
   [[Northern Mariana Islands (the)]]
   MNP,
   [[Norway]]
   NOR,
   [[Oman]]
   OMN,
   [[Pakistan]]
   PAK,
   [[Palau]]
   PLW,
   [[Palestine, State of]]
   PSE,
   [[Panama]]
   PAN,
   [[Papua New Guinea]]
   PNG,
   [[Paraguay]]
   PRY,
   [[Peru]]
   PER,
   [[Philippines (the)]]
   PHL,
   [[Pitcairn]]
   PCN,
   [[Poland]]
   POL,
   [[Portugal]]
   PRT,
   [[Puerto Rico]]
   PRI,
   [[Qatar]]
   QAT,
   [[Republic of North Macedonia]]
   MKD,
   [[Romania]]
   ROU,
   [[Russian Federation (the)]]
   RUS,
   [[Rwanda]]
   RWA,
   [[Réunion]]
   REU,
   [[Saint Barthélemy]]
   BLM,
   [[Saint Helena, Ascension and Tristan da Cunha]]
   SHN,
   [[Saint Kitts and Nevis]]
   KNA,
   [[Saint Lucia]]
   LCA,
   [[Saint Martin (French part)]]
   MAF,
   [[Saint Pierre and Miquelon]]
   SPM,
   [[Saint Vincent and the Grenadines]]
   VCT,
   [[Samoa]]
   WSM,
   [[San Marino]]
   SMR,
   [[Sao Tome and Principe]]
   STP,
   [[Saudi Arabia]]
   SAU,
   [[Senegal]]
   SEN,
   [[Serbia]]
   SRB,
   [[Seychelles]]
   SYC,
   [[Sierra Leone]]
   SLE,
   [[Singapore]]
   SGP,
   [[Sint Maarten (Dutch part)]]
   SXM,
   [[Slovakia]]
   SVK,
   [[Slovenia]]
   SVN,
   [[Solomon Islands]]
   SLB,
   [[Somalia]]
   SOM,
   [[South Africa]]
   ZAF,
   [[South Georgia and the South Sandwich Islands]]
   SGS,
   [[South Sudan]]
   SSD,
   [[Spain]]
   ESP,
   [[Sri Lanka]]
   LKA,
   [[Sudan (the)]]
   SDN,
   [[Suriname]]
   SUR,
   [[Svalbard and Jan Mayen]]
   SJM,
   [[Sweden]]
   SWE,
   [[Switzerland]]
   CHE,
   [[Syrian Arab Republic]]
   SYR,
   [[Taiwan (Province of China)]]
   TWN,
   [[Tajikistan]]
   TJK,
   [[Tanzania, United Republic of]]
   TZA,
   [[Thailand]]
   THA,
   [[Timor-Leste]]
   TLS,
   [[Togo]]
   TGO,
   [[Tokelau]]
   TKL,
   [[Tonga]]
   TON,
   [[Trinidad and Tobago]]
   TTO,
   [[Tunisia]]
   TUN,
   [[Turkey]]
   TUR,
   [[Turkmenistan]]
   TKM,
   [[Turks and Caicos Islands (the)]]
   TCA,
   [[Tuvalu]]
   TUV,
   [[Uganda]]
   UGA,
   [[Ukraine]]
   UKR,
   [[United Arab Emirates (the)]]
   ARE,
   [[United Kingdom of Great Britain and Northern Ireland (the)]]
   GBR,
   [[United States Minor Outlying Islands (the)]]
   UMI,
   [[United States of America (the)]]
   USA,
   [[Uruguay]]
   URY,
   [[Uzbekistan]]
   UZB,
   [[Vanuatu]]
   VUT,
   [[Venezuela (Bolivarian Republic of)]]
   VEN,
   [[Viet Nam]]
   VNM,
   [[Virgin Islands (British)]]
   VGB,
   [[Virgin Islands (U.S.)]]
   VIR,
   [[Wallis and Futuna]]
   WLF,
   [[Western Sahara]]
   ESH,
   [[Yemen]]
   YEM,
   ZMB,
   ZWE,
   ALA
   }

   lenient enum Leg1DayCountFraction {
       OneOverOne("1/1"),
       ThirtyOver360("30/360"),
       ThirtyEOver360("30E/360"),
       ThirtyEOver360Isda("30E/360.ISDA"),
       ActOver360("ACT/360"),
       ActOver365Fixed("ACT/365.FIXED"),
       ActOver365L("ACT/365L"),
       ActOverActAFB("ACT/ACT.AFB"),
       ActOverActICMA("ACT/ACT.ICMA"),
       ActOverActISDA("ACT/ACT.ISDA"),
       ActOverActISMA("ACT/ACT.ISMA"),
       BusinessDaysOverTwoFiveTwo("Bus/252")
   }
lenient enum Leg2DayCountFraction {
    OneOverOne("1/1"),
    ThirtyOver360("30/360"),
    ThirtyEOver360("30E/360"),
    ThirtyEOver360Isda("30E/360.ISDA"),
    ActOver360("ACT/360"),
    ActOver365Fixed("ACT/365.FIXED"),
    ActOver365L("ACT/365L"),
    ActOverActAFB("ACT/ACT.AFB"),
    ActOverActICMA("ACT/ACT.ICMA"),
    ActOverActISDA("ACT/ACT.ISDA"),
    ActOverActISMA("ACT/ACT.ISMA"),
    BusinessDaysOverTwoFiveTwo("Bus/252")
}

enum PUID {
   FxSpot(919),
   SwapXibor(519),
   BondFuture(1643),
   InflationSwap(892),
   CalendarSpread(1732),
   FxEuropeanOption(622),
   FxDeliverableSwap(920),
   FxStrategyButtery(1734),
   FxSimpleStrategy(1923),
   ConventionalBond(2823),
   InflationBond(2245),
   SovereignBond(74),
   CorporateBond(78),
   InflationLinkedSovereignBond(899),
   DeliverableForward(922),
   SwaptionXiborStraddle(1672),
   SovereignBill(1659),
   OvernightIndexSwap(788),
   ZeroCouponInflationFixedFloatSwap(892),
   CrossCurrencySwap(189),
   ConvertibleBond(217)
}

enum ExerciseStyle {
European,
American,
Bermuda
}

enum PutOrCall {
Put,
Call
}

enum DeliveryType {
PHYS,
CASH,
OPTL
}


      type Exchange inherits String
      type  UnitMultiplier inherits Decimal
      type NotionalQuantityRequired inherits Decimal
      type FilledNotional inherits Decimal
      type IdentifierDomain inherits String
      type alias IdentifierType as IdentifierDomain
      type BrokerLimitPrice inherits Decimal
      type BrokerStopPrice inherits Decimal
      type OrderId inherits IdentifierValue
      type BrokerOrderId inherits OrderId
      type OrderEventDateTime inherits Instant
      type OrderEventDate inherits Date
      type IdentifierValue inherits String
      type CfiCode inherits IdentifierValue
      type BrokerCfiCode inherits CfiCode
      type InstrumentId  inherits IdentifierValue
      type StrategyInstrumentId inherits InstrumentId
      type InstrumentIdentifierType inherits IdentifierType
      type SecurityDescription inherits String
      type Isin inherits IdentifierValue
      type PriceAmount inherits Decimal
      type OrderPriceAmount inherits PriceAmount
      type StopPriceAmount inherits PriceAmount
      type Quantity inherits Decimal
      type QuantityHit inherits Decimal
      type QuantityFill inherits Decimal
      type QuantityRequired inherits Decimal
      type NotionalQuantity inherits Decimal
      type TradedNotionalQuantity inherits NotionalQuantity
      enum OrderCurrencyCode inherits CurrencyCode
      type OrderSourceSystemName inherits String
      type Rate inherits Decimal
      type Leg1Rate inherits Rate
      type Leg2Rate inherits Rate
      type BankTraderBrokerLogin inherits String
      type BrokerTraderBrokerLogin inherits BankTraderBrokerLogin
      type MIC inherits String
      type BrokerMic inherits MIC
      type UnderlyingInstrumentIdentifierType inherits IdentifierType
      type UnderlyingInstrumentId inherits InstrumentId
      type Leg1Notional inherits NotionalQuantity
      type Leg1OrigCurrNotionalAmount inherits NotionalQuantity
      type Leg2Notional inherits NotionalQuantity
      type Leg2OrigCurrNotionalAmount inherits NotionalQuantity
      type ClientId inherits String
      type LegalEntityIdentifier inherits String
      type CounterpartyLegalEntityIdentifier inherits LegalEntityIdentifier
      type PartyName inherits String
      type CounterpartyName inherits PartyName
      type TradeVersion inherits Int
      type BankSubAssetClass inherits String
      type MarketTradeId inherits IdentifierValue
      type UtCode inherits String
      type TraderId inherits UtCode
      type Desk inherits String
      type AccountId inherits IdentifierValue
      type CDR inherits String
      type StrategyDescription inherits String
      type UnderlyingIndexName inherits String
      type MaturityDateDate inherits Date
      type MaturityDate inherits DateTime
      type AdjustedDate inherits DateTime
      type AdjustedMaturityDate inherits AdjustedDate
      type ExpiryDate inherits DateTime
      type alias DateStop  as ExpiryDate
      type EffectiveDate inherits DateTime
      type alias DateStart as EffectiveDate
      type DateStopAdjusted inherits AdjustedDate
      type PaymentFrequencyStr inherits String
      type Leg1PaymentFrequency inherits PaymentFrequencyStr
      type ResetFrequencyStr inherits String
      type Leg1ResetFrequency inherits ResetFrequencyStr
      type RateSpread inherits Decimal
      type Leg2RateSpread inherits RateSpread
      type Leg1RateSpread inherits RateSpread
      type Leg2RateSpread inherits RateSpread
      type Leg2PaymentFrequency inherits PaymentFrequencyStr
      type Leg2ResetFrequency inherits ResetFrequencyStr
      type DateAdjustment inherits Decimal
      type Leg2MaturityDateAdjustment inherits DateAdjustment
      type Leg1MaturityDateAdjustment inherits DateAdjustment
      type ProductName inherits String
      type PayOffFamily inherits String
      type PayOffSubFamily inherits String
      type BackOfficeTradeReference inherits IdentifierValue
      type OrderVersion inherits Int
      type StrikePrice inherits PriceAmount
      type Tenor inherits String

      @Datasource
      service OrderService {
           operation findAll(): broker.orders.Order[]
      }

      model Order {
      	@Indexed
      	@Id
      	icapOrderId: BrokerOrderId by column("OrderIdentificationCode")
      	entryType: BrokerOrderStatus by column("Lifecycle")
      	@Between
      	orderDateTime: OrderEventDateTime( @format = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
      	@format = "yyyy-MM-dd'T'HH:mm:ss'Z'",
      	@format = "yyyy-MM-dd'T'HH:mm:ss.S'Z'",
      	@format = "yyyy-MM-dd'T'HH:mm:ss.SS'Z'",
      	@format = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
      	@format = "yyyy-MM-dd'T'HH:mm:ss.SSSS'Z'",
      	@format = "yyyy-MM-dd'T'HH:mm:ss.SSSSS'Z'"
      	) by column("DateAndTime")
      	@Between
      	orderDate: OrderEventDate?( @format = "yyyy-MM-dd", @format = "dd-MM-yyyy" ) by column("DateOfReceipt")
      	venueOrderStatus: BrokerVenueOrderStatus by column("Lifecycle")
      	cfiCode: BrokerCfiCode? by column("InstrumentClassification")
      	identifierValue: StrategyInstrumentId? by column("FinancialInstrumentsIdentificationCode")
      	identifierType: InstrumentIdentifierType by default ("ISIN")
      	isin: Isin? by column ("FinancialInstrumentsIdentificationCode")
      	subSecurityType: SecurityDescription? by column("InstrumentFullName")
      	priceAmount: OrderPriceAmount? by column("LimitPrice")
      	stopPrice: BrokerStopPrice? by column("StopPrice")
      	priceType: BrokerPriceType by column("PriceNotation")
      	quantity: Quantity by column("InitialQuantity")
      	quantityNotation: BrokerQuantityNotation by column("QuantityNotation")
      	quantityRequired: QuantityRequired by column("InitialQuantity")
      	qtyHit : QuantityHit? by column("TradedQuantity")
      	unitMultiplier: UnitMultiplier? by default(1)
      	tradedQuantity: TradedNotionalQuantity? by column ("TradedQuantity")
      	notionalRequired: NotionalQuantityRequired? as (QuantityRequired * UnitMultiplier)
      	filledNotional: FilledNotional? as (QuantityHit * UnitMultiplier)
      	quantityCurrency: OrderCurrencyCode by column("QuantityCurrency")
      	orderType: OrderType by column("OrderType")
      	buySellIndicator: BrokerOrderBankDirection by column("BuySellIndicator")
      	orderValidityPeriod: ValidityPeriod by column("ValidityPeriod")
      	exchange: Exchange by column("MicCode")
      	sourceSystem: OrderSourceSystemName? by column ("MicCode")
      	tempPayReceive: String? by concat(column("InstrumentClassification"),"-",column("BuySellIndicator"))
      	leg1PayReceive: BrokerLeg1PayReceive? by when(this.tempPayReceive) {
      		  "SRCCSP-BUYI" -> Leg1BankPayReceive.Pay
      		  "SRCCSP-SELL" -> Leg1BankPayReceive.Receive
                else -> null
          }
      	leg2PayReceive: BrokerLeg2PayReceive? by when(this.tempPayReceive) {
      		"SRCCSP-BUYI" -> Leg2BankPayReceive.Receive
      		"SRCCSP-SELL" -> Leg2BankPayReceive.Pay
              else -> null
      	}
      	tempLegRate: String? by concat(column("InstrumentClassification"),"-",column("BuySellIndicator"))
      	leg1Rate: Leg1Rate? by when (this.tempLegRate) {
          	"SRCCSP-BUYI" -> column ("LimitPrice")
          	else -> null
          }
          leg2Rate: Leg2Rate? by when (this.tempLegRate){
          	"SRCCSP-SELL" -> column ("LimitPrice")
          	else -> null
          }
      	@Association
      	trader: BrokerTraderBrokerLogin? by column("InvestmentDecisionWithinFirm")
      	brokerVenue: BrokerMic by column("MicCode")
      	underlyingIdentifierType: UnderlyingInstrumentIdentifierType by default ("ISIN")
      	underlyingIdentifierValue : UnderlyingInstrumentId? by column("UnderlyingInstrumentCode")
      	tempLegs: String? by column("InstrumentClassification")
      	leg1NotionalValue: Leg1Notional? by when(this.tempLegs) {
      		"SRCCSP" -> column("InitialQuantity")
      	     else -> null
      	}
         	leg1OrigCurrNotionalAmount: Leg1OrigCurrNotionalAmount? by when(this.tempLegs) {
      		"SRCCSP" -> column("InitialQuantity")
      	     else -> null
      	}
      	leg2NotionalValue: Leg2Notional? by when(this.tempLegs) {
      		"SRCCSP" -> column("InitialQuantity")
      	     else -> null
      	}
         	leg2OrigCurrNotionalAmount: Leg2OrigCurrNotionalAmount? by when(this.tempLegs) {
      		"SRCCSP" -> column("InitialQuantity")
      	     else -> null
      	}
      	leg2Currency: Leg2Currency? by column ("CurrencyOfLeg2")
         method: OrderMethod by default ("Voice")
      	activityCategory: OrderActivityCategory by default ("Hedge")
      	clientid: ClientId by default("CI0000041353")
      	counterpartyLei : CounterpartyLegalEntityIdentifier by default("12340076S228I25PD704")
      	counterParty : CounterpartyName? by default("BROKER EUROPE LIMITED")
      }

      model Report {
         tradeNo : MarketTradeId?
         tradeVersion : TradeVersion?
         orderNo: OrderId?
         orderEntryType: OrderStatus?
         tradeEntryType: TradeFillStatus?
         transactionEventDateTime : OrderEventDateTime?( @format = "dd/MM/yyyy HH:mm:ss")
         secType : BankAssetClass?
         secTypeSub: BankSubAssetClass?
         identifierType: InstrumentIdentifierType?
         identifierValue: StrategyInstrumentId?
         @FirstNotEmpty isin: Isin?
         @FirstNotEmpty securityDescription : SecurityDescription?
         price: OrderPriceAmount?
         priceType : PriceType?
         quantity: Quantity?
         quantityType: QuantityType?
         qtyReq : QuantityRequired?
         qtyHit : QuantityHit?
         unitMultiplier: UnitMultiplier?
         tradedQuantity: TradedNotionalQuantity?
         notionalRequired : NotionalQuantityRequired? as (QuantityRequired * UnitMultiplier)
         filledNotional : FilledNotional?  as (QuantityHit * UnitMultiplier)
         @FirstNotEmpty currency : OrderCurrencyCode?
         orderType: OrderType?
         buySellFlag :  OrderBankDirection?
         tif : TimeInForce?
         exchange : Exchange?
         country :  CountryCode?
         trader : TraderId?
         desk : Desk?
         accountKey: AccountId?
         aggUnitCd : CDR?
         broker: BrokerMic?
         strategy : StrategyDescription?
         underlyingIdentifierType : UnderlyingInstrumentIdentifierType?
         underlyingIdentifierValue : UnderlyingInstrumentId?
         orderMethod: OrderMethod?
         @FirstNotEmpty underlyingIndexName : UnderlyingIndexName ?
         @FirstNotEmpty maturityDate : MaturityDateDate?
         maturityDateTime : MaturityDate?( @format ="yyyy-MM-dd'T'HH:mm:ss" )
         maturtyDateAdj :  AdjustedMaturityDate?( @format ="yyyy-MM-dd'T'HH:mm:ss" )
         dateStart : DateStart?( @format ="yyyy-MM-dd'T'HH:mm:ss" )
         dateStop:  DateStop?( @format ="yyyy-MM-dd'T'HH:mm:ss" )
         dateStopAdjusted :  DateStopAdjusted?( @format ="yyyy-MM-dd'T'HH:mm:ss" )
         activityCategory : OrderActivityCategory?
         counterpartyLei : CounterpartyLegalEntityIdentifier?
         counterParty : CounterpartyName?
         sourceSystem : OrderSourceSystemName?
         venueOrderStatus: VenueOrderStatus?
         leg1DayCountMethodInd : Leg1DayCountFraction?
         @FirstNotEmpty leg1NotionalCurrencyCd : OrderCurrencyCode?
         leg1NotionalValue : Leg1Notional?
         leg1OrigCurrNotionalAmount : Leg1OrigCurrNotionalAmount?
         leg1PaymentFrequency : Leg1PaymentFrequency?
         leg1Rate : Leg1Rate?
         leg1RateSpread : Leg1RateSpread?
         leg1ResetFrequency1 : Leg1ResetFrequency?
         leg1PayReceive: Leg1BankPayReceive?
         leg1MaturityDateAdjustment : Leg1MaturityDateAdjustment?
         @FirstNotEmpty leg2Currency : OrderCurrencyCode?
         leg2DayCountMethodInd : Leg2DayCountFraction?
         leg2NotionalCurrencyCd : Leg2Currency?
         leg2NotionalValue : Leg2Notional?
         leg2OrigCurrNotionalAmount : Leg2OrigCurrNotionalAmount?
         leg2PaymentFrequency2 : Leg2PaymentFrequency?
         leg2Rate : Leg2Rate?
         leg2RateSpread : Leg2RateSpread?
         leg2ResetFrequency2 : Leg2ResetFrequency?
         leg2PayReceive: Leg2BankPayReceive?
         leg2MaturityDateAdjustment : Leg2MaturityDateAdjustment?
         @FirstNotEmpty puid: PUID?
         productName : ProductName?
         payOffFamily : PayOffFamily?
         payOffSubFamily : PayOffSubFamily?
         backOfficeReference : BackOfficeTradeReference?
         orderVersion: OrderVersion?
         @FirstNotEmpty optionType : ExerciseStyle?
         @FirstNotEmpty putCallFag : PutOrCall?
         strikePrice : StrikePrice?
         quantityTraded : QuantityFill?
         tenor : Tenor?
         deliveryType : DeliveryType?
      }


   """.trimIndent()

   @Test
   fun `projection performance for 350 items with trimmed schema`() {
      val (vyne, stubService) = testVyne(schemaStr)
      //findAll
      stubService.addResponse("findAll", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            return vyne.addJsonModel("broker.orders.Order[]", returnJson)
         }
      })

      val result = runBlocking {vyne.query("""
         findAll {
            broker.orders.Order[]
         } as broker.orders.Report[]""".trimIndent())}
      result.isFullyResolved.should.be.`true`
   }

   @Test
   fun `projection performance for 350 items with full schema`() {
      val (vyne, stubService) = testVyne(schema)
      //findAll
      stubService.addResponse("findAll", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            return vyne.addJsonModel("icap.orders.Order[]", returnJson)
         }
      })

      val result = runBlocking {vyne.query("""
         findAll {
            icap.orders.Order[]
         } as cacib.imad.Order[]""".trimIndent())}
      result.isFullyResolved.should.be.`true`
   }

   @Test
   fun `projection performance for 2 items with full schema`() {
      val (vyne, stubService) = testVyne(schema)
      //findAll
      stubService.addResponse("findAll", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            return vyne.addJsonModel("icap.orders.Order[]", twoItems)
         }
      })

      val result = runBlocking {vyne.query("""
         findAll {
            icap.orders.Order[]
         } as cacib.imad.Order[]""".trimIndent())}
      result.isFullyResolved.should.be.`true`
   }

   @Test
   fun `projection performance for 2 items with trimmed schema`() {
      val (vyne, stubService) = testVyne(schemaStr)
      //findAll
      stubService.addResponse("findAll", object : StubResponseHandler {
         override fun invoke(operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            return vyne.addJsonModel("broker.orders.Order[]", twoItems)
         }
      })

      val result = runBlocking {vyne.query("""
         findAll {
            broker.orders.Order[]
         } as broker.orders.Report[]""".trimIndent()) }
      result.isFullyResolved.should.be.`true`
   }


   @Test
   fun investigate() {
      val schema = TaxiSchema.forPackageAtPath(Paths.get("/home/marty/dev/cacib/taxonomy"))
      val (vyne, stub) = testVyne(schema)
      val csv = Paths.get("/home/marty/Documents/demo-files/all_sb2.csv").toFile().readText()
      val orders = TypedCollection.from(CsvImporterUtil.parseCsvToType(csv, CsvIngestionParameters(), schema, "bgc.orders.Order")
         .map { it.instance })
       Benchmark.benchmark("Projecting orders", warmup = 5, iterations = 10) {
          runBlocking {vyne.from(orders).build("cacib.imad.Order[]")}
      }
   }
}

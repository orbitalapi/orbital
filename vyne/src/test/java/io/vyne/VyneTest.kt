package io.vyne

import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedValue
import io.vyne.models.json.addJsonModel
import io.vyne.models.json.addKeyValuePair
import io.vyne.models.json.parseJsonModel
import io.vyne.query.*
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.PropertyToParameterConstraint
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.Operator
import lang.taxi.services.operations.constraints.ConstantValueExpression
import lang.taxi.services.operations.constraints.PropertyTypeIdentifier
import lang.taxi.types.QualifiedName
import org.junit.Ignore
import org.junit.Test
import java.time.LocalDate
import kotlin.test.fail

object TestSchema {
   val taxiDef = """
namespace vyne.example
type Invoice {
   clientId : ClientId
   invoiceValue : InvoiceValue as Decimal
}
type Client {
   clientId : ClientId as String
   name : ClientName as String
   isicCode : IsicCode as String
}
type alias TaxFileNumber as String
type alias CreditRisk as Int
type alias NaicsCode as Int

service ClientService {
   @StubResponse("mockClient")
   operation getClient(TaxFileNumber):Client

   @StubResponse("creditRisk")
   operation getCreditRisk(ClientId,InvoiceValue):CreditRisk

   @StubResponse("mockClients")
   operation getClients(NaicsCode):Client[]
}
"""
   val schema = TaxiSchema.from(taxiDef)


   fun vyne(
      queryEngineFactory: QueryEngineFactory = QueryEngineFactory.default(),
      testSchema: TaxiSchema = schema) = Vyne(queryEngineFactory).addSchema(testSchema)
   val queryParser = QueryParser(schema)

   fun typeNode(name: String): Set<QuerySpecTypeNode> {
      return queryParser.parse(TypeNameQueryExpression(name))
   }

   fun queryContext(): QueryContext = vyne().queryEngine().queryContext()
}

fun testVyne(schema: TaxiSchema): Pair<Vyne, StubService> {
   val stubService = StubService()
   val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
   val vyne = Vyne(queryEngineFactory).addSchema(schema)
   return vyne to stubService
}

fun testVyne(schema: String) = testVyne(TaxiSchema.from(schema))

class VyneTest {


   @Test
   fun shouldFindAPropertyOnAnObject() {

      val vyne = TestSchema.vyne()
      val json = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""
      vyne.addJsonModel("vyne.example.Client", json)
      val result = vyne.query().find("vyne.example.ClientName")
      expect(result.results.size).to.equal(1)
      expect(result["vyne.example.ClientName"]!!.value).to.equal("Jimmy's Choos")
   }

   @Test
   fun `vyne should emit values that conform to the enum spec`() {
      val enumSchema = TaxiSchema.from("""
                namespace companyX {
                   model Product {
                     name : String
                  }
                  enum ProductType {
                     SPOT(919),
                     FORWARD(920)
                  }
                  service ProductTaxonomyService {
                     @StubResponse("mockProduct")
                     operation getProduct(ProductType):Product
                  }
                }
                namespace vendorA {
                   enum ProductType {
                      FX_SPOT("Spot") synonym of companyX.ProductType.SPOT
                   }
                }

      """.trimIndent())

      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      val vyne = TestSchema.vyne(queryEngineFactory, enumSchema)
      val product = vyne.parseJsonModel("companyX.Product", """
         {
            "name": "USD/GBP"
         }
      """.trimIndent())
      stubService.addResponse("mockProduct", object : StubResponseHandler {
         override fun invoke(operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(1)
            parameters.first().second.value.should.be.equal(919)
            return product
         }
      })
      val instance = TypedInstance.from(vyne.schema.type("vendorA.ProductType"), "Spot", vyne.schema)
      vyne.addModel(instance)
      val queryResult = vyne.query().find("companyX.Product")
      expect(queryResult.results.size).to.equal(1)
      val attributeMap = queryResult["companyX.Product"]!!.value as Map<String, TypedValue>
      expect((attributeMap["name"] ?: error("")).value).to.equal("USD/GBP")
   }

   @Test
   fun `vyne should emit values transitively that conform to the enum spec`() {
      val enumSchema = TaxiSchema.from("""
                namespace companyY {
                   model Product {
                     name : String
                  }

                  enum ProductClassification {
                     T_PLUS_2("FX_T2"),
                     T_PLUS_N("FX_TN")
                  }

                  service ProductTaxonomyService {
                     @StubResponse("mockProduct")
                     operation getProduct(ProductClassification):Product
                  }
                }
                namespace companyX {
                   model Product {
                     name : String
                  }
                  enum ProductType {
                     SPOT(919) synonym of companyY.ProductClassification.T_PLUS_2,
                     FORWARD(920)
                  }
                }
                namespace vendorA {
                   enum ProductType {
                      FX_SPOT("Spot") synonym of companyX.ProductType.SPOT
                   }
                }

      """.trimIndent())

      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      val vyne = TestSchema.vyne(queryEngineFactory, enumSchema)
      val product = vyne.parseJsonModel("companyY.Product", """
         {
            "name": "USD/GBP"
         }
      """.trimIndent())
      stubService.addResponse("mockProduct", object : StubResponseHandler {
         override fun invoke(operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(1)
            parameters.first().second.value.should.be.equal("FX_T2")
            return product
         }
      })
      val instance = TypedInstance.from(vyne.schema.type("vendorA.ProductType"), "Spot", vyne.schema)
      vyne.addModel(instance)
      val queryResult = vyne.query().find("companyY.Product")
      expect(queryResult.results.size).to.equal(1)
      val attributeMap = queryResult["companyY.Product"]!!.value as Map<String, TypedValue>
      expect((attributeMap["name"] ?: error("")).value).to.equal("USD/GBP")
   }

   @Test
   fun shouldRetrievePropertyFromService() {
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      val vyne = TestSchema.vyne(queryEngineFactory)

      val json = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""
      val client = vyne.parseJsonModel("vyne.example.Client", json)
      stubService.addResponse("mockClient", client)
      vyne.addKeyValuePair("vyne.example.TaxFileNumber", "123")
      val result: QueryResult = vyne.query().find("vyne.example.ClientName")
      expect(result.results.size).to.equal(1)
      expect(result["vyne.example.ClientName"]!!.value).to.equal("Jimmy's Choos")
   }

   @Test
   fun shouldBeAbleToQueryWithShortNames() {
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      val vyne = TestSchema.vyne(queryEngineFactory)

      val json = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""
      val client = vyne.parseJsonModel("Client", json)
      stubService.addResponse("mockClient", client)
      vyne.addKeyValuePair("vyne.example.TaxFileNumber", "123")
      val result: QueryResult = vyne.query().find("ClientName")
      expect(result.results.size).to.equal(1)
      expect(result["vyne.example.ClientName"]!!.value).to.equal("Jimmy's Choos")
   }

   @Test
   fun shouldRetrievePropertyFromService_withMultipleAttributes_whenAttributesArePresentAsKeyValuePairs() {
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      val vyne = TestSchema.vyne(queryEngineFactory)
      stubService.addResponse("creditRisk", TypedValue.from(vyne.getType("vyne.example.CreditRisk"), 100))
      vyne.addKeyValuePair("vyne.example.ClientId", "123")
      vyne.addKeyValuePair("vyne.example.InvoiceValue", 1000)
      val result: QueryResult = vyne.query().find("vyne.example.CreditRisk")
      expect(result.results.size).to.equal(1)
      expect(result["vyne.example.CreditRisk"]!!.value).to.equal(100)
      val paramsPassedToService: List<TypedInstance> = stubService.invocations["creditRisk"]!!
      expect(paramsPassedToService).size(2)
      expect(paramsPassedToService[0].value).to.equal("123")
      expect(paramsPassedToService[1].value).to.equal(1000.toBigDecimal())
   }


   @Test
   @Ignore // Failing, requires investigation.
   // This test is currently failing, and needs looking into.
   // It apepars that the stubbed operation that should find the clientId from the mockService
   // isn't getting invoked when doing param discovery.
   // This happens frequently, but sometimes it does get invoked, making the test flakey.
   // The test more repeatably passes when run in isolation, using the setup code that has been
   // commented out.
   fun shouldRetrievePropertyFromService_withMultipleAttributes_whenAttributesAreDiscoverableViaGraph() {
      // Setup
//      val stubService = StubService()
//      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
//      val vyne = TestSchema.vyne(queryEngineFactory)
      val (vyne, stubService) = testVyne(TestSchema.schema)

      // Given...
      val json = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""
      stubService.addResponse("creditRisk", TypedValue.from(vyne.getType("vyne.example.CreditRisk"), 100))

      val client = vyne.parseJsonModel("vyne.example.Client", json)
      stubService.addResponse("mockClient", client)

      // We know the TaxFileNumber, which we should be able to use to discover their ClientId.
      vyne.addKeyValuePair("vyne.example.TaxFileNumber", "123")
      vyne.addKeyValuePair("vyne.example.InvoiceValue", 1000)

      //When....
      val result: QueryResult = try {
         vyne.query().find("vyne.example.CreditRisk")
      } catch (e: Exception) {
         fail()
      }


      // Then....
      expect(result.results.size).to.equal(1)
      expect(result["vyne.example.CreditRisk"]!!.value).to.equal(100)
      val paramsPassedToService: List<TypedInstance> = stubService.invocations["creditRisk"]!!
      expect(paramsPassedToService).size(2)
      expect(paramsPassedToService[0].value).to.equal("123")
      expect(paramsPassedToService[1].value).to.equal(1000.toBigDecimal())
   }


   @Test
   @Ignore
   fun given_notAllParamsOfOperationAreDiscoverable_then_methodNotInvoked() {
      TODO("Not sure what we should do here.")
   }

   @Test
   @Ignore
   // This test is disabled, as we don't really provide a way
   // in the query api to submit a 'start' node for the query.
   // Instead, we're just building a bag of facts, and then
   // asking for a result.
   // Therefore, this test would simply grab the name directly
   // from the context, which isn't what it was trying to do.
   // Not sure if we should allow a more specific API.
   fun shouldFindAPropertyValueByWalkingADirectRelationship() {

      val vyne = TestSchema.vyne()
      val client = """
         {
            "clientId" : "123",
            "name" : "Jimmy's Choos",
            "isicCode" : "retailer"
         }"""
      val invoice = """
         {
            "clientId" : "123"
         }
         """
      vyne.addJsonModel(typeName = "vyne.example.Client", json = client)

      val invoiceInstance = vyne.parseJsonModel(typeName = "vyne.example.Invoice", json = invoice)
      // The below line isn't implemented, and isn't trivial to do so, as it involves us remodelleding
      // the query api to provide an explicit start point.
      val result = vyne.from(invoiceInstance).find("vyne.example.ClientName")
      expect(result["vyne.example.ClientName"]!!.value).to.equal("Jimmy's Choos")
   }

   @Test
   fun typeInheritsCanBeUsedInSingleDirectionToAssignValues() {
      val schema = """
          type Money {
            amount:String
          }
          type TradeValue inherits Money

          type alias HoldReceipt as String
          service LedgerService {
            operation holdFunds(Money):HoldReceipt
          }
      """.trimIndent()

      // In the above, I should be able to get a HoldReceipt by calling
      // holdFunds() using the TradeValue, which is a type of Money

      val (vyne, stubService) = testVyne(schema)
      val tradeValue = vyne.parseJsonModel("TradeValue", """{ "amount" : "$2.00" }""")
      stubService.addResponse("holdFunds", vyne.typedValue("HoldReceipt", "held-123"))
      val result = vyne.query(additionalFacts = setOf(tradeValue)).find("HoldReceipt")

      expect(result.isFullyResolved).to.be.`true`
      expect(result["HoldReceipt"]!!.value).to.equal("held-123")
   }

   @Test
   fun given_targetTypeIsExposedDirectlyByService_then_canDiscoverWithNoStartPoint() {
      val schema = """
          type alias EmailAddress as String
          service CustomerService {
            operation singleEmail():EmailAddress
          }
      """.trimIndent()

      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("singleEmail", vyne.typedValue("EmailAddress", "foo@foo.com"))

      val result = vyne.query().find("EmailAddress")

      expect(result.isFullyResolved).to.be.`true`
      expect(result["EmailAddress"]!!.value).to.equal("foo@foo.com")
   }

   @Test
   fun canRequestListTypeDirectlyFromService() {
      val schema = """
          type Customer {
            emails : EmailAddress[]
          }
          type alias EmailAddresses as EmailAddress[]
          type alias EmailAddress as String
          service CustomerService {
            operation emails():EmailAddress[]
          }
      """.trimIndent()

      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("emails", vyne.typedValue("EmailAddress[]", listOf("foo@foo.com", "bar@foo.com")))

      val result = vyne.query().find("EmailAddress[]")

      expect(result.isFullyResolved).to.be.`true`
      expect(result.resultMap["EmailAddress[]".fqn().parameterizedName]).to.equal(listOf("foo@foo.com", "bar@foo.com"))
   }

   @Test
   fun canRequestTypeAliasOfCollectionDirectlyFromService() {
      val schema = """
          type Customer {
            emails : EmailAddress[]
          }
          type alias EmailAddresses as EmailAddress[]
          type alias EmailAddress as String
          service CustomerService {
            operation emails():EmailAddresses
          }
      """.trimIndent()

      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("emails", vyne.typedValue("EmailAddresses", listOf("foo@foo.com", "bar@foo.com")))

      val result = vyne.query().find("EmailAddresses")

      expect(result.isFullyResolved).to.be.`true`
      expect(result.resultMap["EmailAddresses".fqn().parameterizedName]).to.equal(listOf("foo@foo.com", "bar@foo.com"))

      // Discovery by the aliases type name should work too
      val resultFromAliasName = vyne.query().find("EmailAddress[]")
      expect(resultFromAliasName.isFullyResolved).to.be.`true`
      expect(resultFromAliasName.resultMap["EmailAddress[]".fqn().parameterizedName]).to.equal(listOf("foo@foo.com", "bar@foo.com"))

   }

   @Test
   fun canDiscoverAliasedTypesWhenUsingGraphDiscoveryStrategy() {
      val schema = """
          type Customer {
            name : CustomerName as String
            emails : EmailAddresses
          }
          type alias EmailAddresses as EmailAddress[]
          type alias EmailAddress as String
          type alias Region as String
          type alias CustomerList as Customer[]
          service CustomerService {
            operation customersInRegion(Region):CustomerList
          }
      """.trimIndent()

      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("customersInRegion", vyne.addJsonModel("CustomerList", """
         [
            { "name" : "Jimmy", "emails" : [ "foo@foo.com" ] },
            { "name" : "Jack", "emails" : [ "baz@foo.com" ] }
         ]
         """.trimIndent()))

      listOf("CustomerList", "Customer[]").forEach { typeToDiscover ->
         val result = vyne.query(additionalFacts = setOf(vyne.typedValue("Region", "UK")))
            .find(typeToDiscover)
         result.isFullyResolved.should.equal(true)
         val resultList = result[typeToDiscover] as List<*>
         resultList.should.have.size(2)
      }
   }

   @Test
   fun canInvokeServiceWithParamMatchingOnType() {
      val schema = """
         type Pet {
            id : Int
         }
         service PetService {
            @HttpOperation(method = "GET" , url = "http://petstore.swagger.io/api/pets/{id}")
            operation findPetById(  id : Int ) : Pet
         }
      """.trimIndent()

      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("findPetById", vyne.typedValue("Pet", mapOf("id" to 100)))
      vyne.addKeyValuePair("lang.taxi.Int", 100)
      val result = vyne.query().find("Pet")

      expect(result.isFullyResolved).to.be.`true`
      val params = stubService.invocations["findPetById"]!!.get(0)
      expect(params.value).to.equal(100)
   }

   @Test
   @Ignore("This feature isn't built yet")
   fun given_xmlBlobWithSchema_then_canAccessValeus() {
      val src = """
type Money {
   amount : MoneyAmount as Decimal
   currency : Currency as String
}
type alias Instrument as String
type NearLegNotional inherits Money {}
type FarLegNotional inherits Money {}

type LegacyTradeNotification {
   nearLegNotional : NearLegNotional {
       amount by xpath("/tradeNotification/legs/leg[1]/notional/amount/text()")
       currency by xpath("/tradeNotification/legs/leg[1]/currency/amount/text()")
   }
   farLegNotional : FarLegNotional {
       amount by xpath("/tradeNotification/legs/leg[2]/notional/amount/text()")
       currency by xpath("/tradeNotification/legs/leg[2]/currency/amount/text()")
   }
}
        """.trimIndent()
      val (vyne, stubService) = testVyne(src)
      val xml = """
 <tradeNotification>
    <legs>
        <leg>
            <notional>
                <amount>200000</amount>
                <currency>GBP</currency>
            </notional>
        </leg>
        <leg>
            <notional>
                <amount>700000</amount>
                <currency>GBP</currency>
            </notional>
        </leg>
    </legs>
</tradeNotification>
      """.trimIndent()
      val instance = TypedInstance.from(vyne.schema.type("LegacyTradeNotification"), xml, vyne.schema)
      vyne.addModel(instance)
      val queryResult = vyne.query().find("NearLegNotional")
      TODO()
   }

   val schema = """
type alias OrderDate as Date
type alias OrderId as String

model IMadOrder {
   id: OrderId
   date: OrderDate
}

model Order {
}
type HpcOrder inherits Order {
   hpcID: OrderId
   hpcDate: OrderDate
}
type IonOrder inherits Order {
   ionID: OrderId
   ionDate: OrderDate
}

// operations
service HpcService {
   operation getHpcOrders( start : OrderDate, end : OrderDate) : HpcOrder[] (OrderDate >= start, OrderDate < end)
}
service IonService {
   operation getIonOrders( start : OrderDate, end : OrderDate) : IonOrder[] (OrderDate >= start, OrderDate < end)
}

""".trimIndent()

   @Test
   @Ignore("This doesn't pass any criteria, which is resulting in services that expose criteria not getting invoked.  Not sure what the expected behaviour should be, will revisit")
   fun canGatherOrdersFromTwoDifferentServices() {
      // prepare
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("getHpcOrders", vyne.parseJsonModel("HpcOrder[]", """
         [
            { "hpcID" : "hpcOrder1", "hpcDate" : "2020-01-01"}
         ]
         """.trimIndent()))
      stubService.addResponse("getIonOrders", vyne.parseJsonModel("IonOrder[]", """
         [
            { "ionID" : "ionOrder1", "ionDate" : "2020-01-01"}
         ]
         """.trimIndent()))

      // act
      val result = vyne.query().findAll("Order[]")

      // assert
      expect(result.isFullyResolved).to.be.`true`
      val resultList = result.resultMap.values.map { it as ArrayList<*> }.flatMap { it.asIterable() }.flatMap { it as ArrayList<*> }
      resultList.should.contain.all.elements(
         mapOf(Pair("hpcID", "hpcOrder1"), Pair("hpcDate", LocalDate.parse("2020-01-01"))),
         mapOf(Pair("ionID", "ionOrder1"), Pair("ionDate", LocalDate.parse("2020-01-01")))
      )
   }

   @Test
   fun canGatherOrdersFromTwoDifferentServices_AndFilterByDateRange() {
      // prepare
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("getHpcOrders", vyne.parseJsonModel("HpcOrder[]", """
         [
            { "hpcID" : "hpcOrder1", "hpcDate" : "2020-01-01"},
            { "hpcID" : "hpcOrder2", "hpcDate" : "2020-01-02"}
         ]
         """.trimIndent()))
      stubService.addResponse("getIonOrders", vyne.parseJsonModel("IonOrder[]", """
         [
            { "ionID" : "ionOrder1", "ionDate" : "2020-01-01"},
            { "ionID" : "ionOrder2", "ionDate" : "2020-01-02"}
         ]
         """.trimIndent()))

      // act
      val result = vyne.query().findAll(
         ConstrainedTypeNameQueryExpression("Order[]", listOf(
            PropertyToParameterConstraint(
               PropertyTypeIdentifier(QualifiedName.from("OrderDate")),
               Operator.GREATER_THAN_OR_EQUAL_TO,
               ConstantValueExpression(LocalDate.parse("2020-01-01"))
            ),
            PropertyToParameterConstraint(
               PropertyTypeIdentifier(QualifiedName.from("OrderDate")),
               Operator.LESS_THAN,
               ConstantValueExpression(LocalDate.parse("2020-01-02"))
            )
         ))
      )

      // assert
      expect(result.isFullyResolved).to.be.`true`
      stubService.invocations.should.have.size(2)
      val orders = result["Order[]"] as List<TypedInstance>
      orders.should.have.size(4)
   }

   @Test
   fun canDoFindAllUsingVyneQlQuery() {
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("getHpcOrders", vyne.parseJsonModel("HpcOrder[]", """
         [
            { "hpcID" : "hpcOrder1", "hpcDate" : "2020-01-01"},
            { "hpcID" : "hpcOrder2", "hpcDate" : "2020-01-02"}
         ]
         """.trimIndent()))
      stubService.addResponse("getIonOrders", vyne.parseJsonModel("IonOrder[]", """
         [
            { "ionID" : "ionOrder1", "ionDate" : "2020-01-01"},
            { "ionID" : "ionOrder2", "ionDate" : "2020-01-02"}
         ]
         """.trimIndent()))
      val result = vyne.query("""
         findAll {
            Order[]( OrderDate >="2020-01-01", OrderDate < "2020-01-02" )
         }
      """.trimIndent())
      expect(result.isFullyResolved).to.be.`true`
      stubService.invocations.should.have.size(2)
      val orders = result["Order[]"] as List<TypedInstance>
      orders.should.have.size(4)
   }

   @Test
   fun canProjectDifferentOrderTypesToSingleType() {
      // prepare
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("getHpcOrders", vyne.addJsonModel("HpcOrder[]", """
         [
            { "hpcID" : "hpcOrder1", "hpcDate" : "2020-01-01"}
         ]
         """.trimIndent()))
      stubService.addResponse("getIonOrders", vyne.addJsonModel("IonOrder[]", """
         [
            { "ionID" : "ionOrder1", "ionDate" : "2020-01-01"}
         ]
         """.trimIndent()))

      // act
      val result = vyne.query().projectResultsTo("IMadOrder[]").findAll("Order[]")

      // assert
      expect(result.isFullyResolved).to.be.`true`
      val resultList = result.resultMap.values.map { it as ArrayList<*> }.flatMap { it.asIterable() }
      resultList.should.contain.all.elements(
         mapOf(Pair("id", "hpcOrder1"), Pair("date", LocalDate.parse("2020-01-01"))),
         mapOf(Pair("id", "ionOrder1"), Pair("date", LocalDate.parse("2020-01-01")))
      )
   }

   @Test
   fun canProjectDifferentOrderTypesToSingleType_whenSomeValuesAreMissing() {
      // prepare
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("getHpcOrders", vyne.addJsonModel("HpcOrder[]", """
         [
            { "hpcID" : "hpcOrder1"}
         ]
         """.trimIndent()))
      stubService.addResponse("getIonOrders", vyne.addJsonModel("IonOrder[]", """
         [
            { "ionID" : "ionOrder1", "ionDate" : "2020-01-01"}
         ]
         """.trimIndent()))

      // act
      val result = vyne.query().findAll("Order[]")

      // assert
      expect(result.isFullyResolved).to.be.`true`
      val resultList = result.resultMap.values.map { it as ArrayList<*> }.flatMap { it.asIterable() }.flatMap { it as ArrayList<*> }
      resultList.should.contain.all.elements(
         mapOf(Pair("hpcID", "hpcOrder1")),
         mapOf(Pair("ionID", "ionOrder1"), Pair("ionDate", LocalDate.parse("2020-01-01")))
      )
   }

   @Test
   fun `Can process typed array return values from a service`() {
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      val vyne = TestSchema.vyne(queryEngineFactory)

      val json = """[
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
},
{
   "clientId" : "1234",
   "name" : "Givenchy",
   "isicCode" : "retailer"
}
]"""
      val clients = vyne.parseJsonModel("vyne.example.Client[]", json)
      stubService.addResponse("mockClients", clients)
      vyne.addKeyValuePair("vyne.example.NaicsCode", 911)
      val result: QueryResult = vyne.query().find("vyne.example.Client[]")
      expect(result.results.size).to.equal(1)
      val resultList = (result.resultMap.values.first() as List<LinkedHashMap<*, *>>)
         .flatMap { it.entries }
         .map { Pair(it.key, it.value) }

      resultList.should.contain.all.elements(
         Pair("clientId", "123"),
         Pair("name", "Jimmy's Choos"),
         Pair("isicCode", "retailer"),
         Pair("clientId", "1234"),
         Pair("name", "Givenchy"),
         Pair("isicCode", "retailer")
        )
   }
}

fun Vyne.typedValue(typeName: String, value: Any): TypedInstance {
   return TypedInstance.from(this.getType(typeName), value, this.schema)
//   return TypedValue.from(this.getType(typeName), value)
}



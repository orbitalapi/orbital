package io.vyne

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.*
import io.vyne.models.json.addJsonModel
import io.vyne.models.json.addKeyValuePair
import io.vyne.models.json.parseJsonModel
import io.vyne.models.json.parseKeyValuePair
import io.vyne.query.*
import io.vyne.query.graph.operationInvocation.CacheAwareOperationInvocationDecorator
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.PropertyToParameterConstraint
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.Operator
import lang.taxi.services.operations.constraints.ConstantValueExpression
import lang.taxi.services.operations.constraints.PropertyTypeIdentifier
import lang.taxi.types.QualifiedName
import org.junit.Ignore
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.lang.StringBuilder
import java.time.Instant
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

   fun typeNode(name: String, parser: QueryParser = queryParser): Set<QuerySpecTypeNode> {
      return parser.parse(TypeNameQueryExpression(name))
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
   fun `when a provided object has a typed null for a value, it shouldnt be used as an input`() {
      val (vyne, stubs) = testVyne("""
         model Order {
            cfiCode : CfiCode? as String
            isin : Isin as String
         }
         model Product {
            productId : ProductId as Int
            cfiCode : CfiCode
         }

         model CfiCodeHolder {
            cfiCode: CfiCode
            field: String
         }

         service ProductService {
            // Shortest path, but provided value is null, so shouldn't be called
            @StubOperation("findByCfiCode")
            operation findByCfiCode(CfiCode):Product
            // Longer path, but returns correct value
            @StubOperation("isinToCfi")
            operation isinToCfi(Isin):CfiCodeHolder
         }
      """.trimIndent())
      val inputJson = """{
         |"cfiCode" : null,
         |"isin" : "isin-123"
         |}
      """.trimMargin()
      val input = TypedInstance.from(vyne.type("Order"), inputJson, vyne.schema, source = Provided)

      val cfiCodeHolder = """{
         |"cfiCode": "Cfi-123",
         |"field": "value"
         |}
      """.trimMargin()
      stubs.addResponse("isinToCfi", TypedInstance.from(vyne.type("CfiCodeHolder"), cfiCodeHolder, vyne.schema, source = Provided))
      stubs.addResponse("findByCfiCode") { operation, parameters ->
         val cfiCode = parameters[0].second
         if (cfiCode.value == "Cfi-123") {
            val response = """{
               |"productId" : 123,
               |"cfiCode" : "Cfi-123"
               |}
            """.trimMargin()
            TypedInstance.from(vyne.type("Product"), response, vyne.schema, source = Provided)
         } else {
            fail("findByCfiCode called using the wrong parameter -- should've resolve against Isin first")
         }
      }
      val queryResult = vyne.from(input).find("ProductId")
      queryResult.isFullyResolved.should.be.`true`
      queryResult["ProductId"]!!.value.should.equal(123)
   }

   @Test
   fun `calls remote services to discover response from deeply nested value`() {
      val (vyne, stubs) = testVyne("""
         namespace vyne.tests {
            type Isin inherits String
            type SecurityDescription inherits String
            model InstrumentResponse {
                isin : Isin?
                annaJson : AnnaJson?
            }
            model AnnaJson {
                Derived : Derived?
            }
            model Derived {
                ShortName : SecurityDescription?
            }

            model RequiredOutput {
               isin : Isin?
               description : SecurityDescription?
            }

            service StubService {
               @StubResponse("securityDescription")
               operation getAnnaJson(isin:Isin):InstrumentResponse
            }
         }
      """)
      val stubResponse = TypedInstance.from(vyne.type("vyne.tests.InstrumentResponse"), """
         {
            "isin": "foo",
            "annaJson" : {
               "Derived" : {
                  "ShortName" : "Jimmy's Diner"
               }
            }
         }
      """.trimIndent(), vyne.schema, source = Provided)
      stubs.addResponse("securityDescription", stubResponse)
      vyne.addKeyValuePair("vyne.tests.Isin", "foo")
      val result = vyne.query().build("vyne.tests.RequiredOutput")
      result.isFullyResolved.should.be.`true`
      val rawResult = result["vyne.tests.RequiredOutput"]!!.toRawObject()
      val resultJson = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(rawResult)
      val expected = """{
         | "isin" : "foo",
         | "description" : "Jimmy's Diner"
         | }
      """.trimMargin()
      JSONAssert.assertEquals(expected, resultJson, true)
   }

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
      val instance = TypedInstance.from(vyne.schema.type("vendorA.ProductType"), "Spot", vyne.schema, source = Provided)
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
      val instance = TypedInstance.from(vyne.schema.type("vendorA.ProductType"), "Spot", vyne.schema, source = Provided)
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
      stubService.addResponse("creditRisk", TypedValue.from(vyne.getType("vyne.example.CreditRisk"), 100, source = Provided))
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
      stubService.addResponse("creditRisk", TypedValue.from(vyne.getType("vyne.example.CreditRisk"), 100, source = Provided))

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
      val instance = TypedInstance.from(vyne.schema.type("LegacyTradeNotification"), xml, vyne.schema, source = Provided)
      vyne.addModel(instance)
      val queryResult = vyne.query().find("NearLegNotional")
      TODO()
   }

   val schema = """
type alias OrderDate as Date
type alias OrderId as String

model CommonOrder {
   id: OrderId
   date: OrderDate
}

model Order {
}
type Broker1Order inherits Order {
   broker1ID: OrderId
   broker1Date: OrderDate
}
type Broker2Order inherits Order {
   broker2ID: OrderId
   broker2Date: OrderDate
}

// operations
service Broker1Service {
   operation getBroker1Orders( start : OrderDate, end : OrderDate) : Broker1Order[] (OrderDate >= start, OrderDate < end)
}
service Broker2Service {
   operation getBroker2Orders( start : OrderDate, end : OrderDate) : Broker2Order[] (OrderDate >= start, OrderDate < end)
}

""".trimIndent()

   @Test
   @Ignore("This doesn't pass any criteria, which is resulting in services that expose criteria not getting invoked.  Not sure what the expected behaviour should be, will revisit")
   fun canGatherOrdersFromTwoDifferentServices() {
      // prepare
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("getBroker1Orders", vyne.parseJsonModel("Broker1Order[]", """
         [
            { "broker1ID" : "Broker1Order1", "broker1Date" : "2020-01-01"}
         ]
         """.trimIndent()))
      stubService.addResponse("getBroker2Orders", vyne.parseJsonModel("Broker2Order[]", """
         [
            { "broker2ID" : "Broker2Order1", "broker2Date" : "2020-01-01"}
         ]
         """.trimIndent()))

      // act
      val result = vyne.query().findAll("Order[]")

      // assert
      expect(result.isFullyResolved).to.be.`true`
      val resultList = result.resultMap.values.map { it as ArrayList<*> }.flatMap { it.asIterable() }.flatMap { it as ArrayList<*> }
      resultList.should.contain.all.elements(
         mapOf(Pair("broker1ID", "Broker1Order1"), Pair("broker1Date", LocalDate.parse("2020-01-01"))),
         mapOf(Pair("broker2ID", "Broker2Order1"), Pair("broker2Date", LocalDate.parse("2020-01-01")))
      )
   }

   @Test
   fun canGatherOrdersFromTwoDifferentServices_AndFilterByDateRange() {
      // prepare
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("getBroker1Orders", vyne.parseJsonModel("Broker1Order[]", """
         [
            { "broker1ID" : "Broker1Order1", "broker1Date" : "2020-01-01"},
            { "broker1ID" : "Broker1Order2", "broker1Date" : "2020-01-02"}
         ]
         """.trimIndent()))
      stubService.addResponse("getBroker2Orders", vyne.parseJsonModel("Broker2Order[]", """
         [
            { "broker2ID" : "Broker2Order1", "broker2Date" : "2020-01-01"},
            { "broker2ID" : "Broker2Order2", "broker2Date" : "2020-01-02"}
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
      stubService.addResponse("getBroker1Orders", vyne.parseJsonModel("Broker1Order[]", """
         [
            { "broker1ID" : "Broker1Order1", "broker1Date" : "2020-01-01"},
            { "broker1ID" : "Broker1Order2", "broker1Date" : "2020-01-02"}
         ]
         """.trimIndent()))
      stubService.addResponse("getBroker2Orders", vyne.parseJsonModel("Broker2Order[]", """
         [
            { "broker2ID" : "Broker2Order1", "broker2Date" : "2020-01-01"},
            { "broker2ID" : "Broker2Order2", "broker2Date" : "2020-01-02"}
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
      stubService.addResponse("getBroker1Orders", vyne.addJsonModel("Broker1Order[]", """
         [
            { "broker1ID" : "Broker1Order1", "broker1Date" : "2020-01-01"}
         ]
         """.trimIndent()))
      stubService.addResponse("getBroker2Orders", vyne.addJsonModel("Broker2Order[]", """
         [
            { "broker2ID" : "Broker2Order1", "broker2Date" : "2020-01-01"}
         ]
         """.trimIndent()))

      // act
      val result = vyne.query().projectResultsTo("CommonOrder[]").findAll("Order[]")

      // assert
      expect(result.isFullyResolved).to.be.`true`
      val resultList = result.resultMap.values.map { it as ArrayList<*> }.flatMap { it.asIterable() }
      resultList.should.contain.all.elements(
         mapOf(Pair("id", "Broker1Order1"), Pair("date", "2020-01-01")),
         mapOf(Pair("id", "Broker2Order1"), Pair("date", "2020-01-01"))
      )
   }

   @Test
   fun canProjectDifferentOrderTypesToSingleTypeFromUsingVyneQLQuery() {
      // prepare
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("getBroker1Orders", vyne.addJsonModel("Broker1Order[]", """
         [
            { "broker1ID" : "Broker1Order1", "broker1Date" : "2020-01-01"}
         ]
         """.trimIndent()))
      stubService.addResponse("getBroker2Orders", vyne.addJsonModel("Broker2Order[]", """
         [
            { "broker2ID" : "Broker2Order1", "broker2Date" : "2020-01-01"}
         ]
         """.trimIndent()))

      // act
      val result = vyne.query("""
         findAll { Order[] } as CommonOrder[]
      """.trimIndent())

      // assert
      expect(result.isFullyResolved).to.be.`true`
      val resultList = result.resultMap.values.map { it as ArrayList<*> }.flatMap { it.asIterable() }
      resultList.should.contain.all.elements(
         mapOf(Pair("id", "Broker1Order1"), Pair("date", "2020-01-01")),
         mapOf(Pair("id", "Broker2Order1"), Pair("date", "2020-01-01"))
      )
   }

   @Test
   fun canProjectDifferentOrderTypesToSingleType_whenSomeValuesAreMissing() {
      // prepare
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse("getBroker1Orders", vyne.addJsonModel("Broker1Order[]", """
         [
            { "broker1ID" : "Broker1Order1"}
         ]
         """.trimIndent()))
      stubService.addResponse("getBroker2Orders", vyne.addJsonModel("Broker2Order[]", """
         [
            { "broker2ID" : "Broker2Order1", "broker2Date" : "2020-01-01"}
         ]
         """.trimIndent()))

      // act
      val result = vyne.query().findAll("Order[]")

      // assert
      expect(result.isFullyResolved).to.be.`true`
      val resultList = result.resultMap.values.map { it as ArrayList<*> }.flatMap { it.asIterable() }.flatMap { it as ArrayList<*> }
      resultList.should.contain.all.elements(
         mapOf(Pair("broker1ID", "Broker1Order1")),
         mapOf(Pair("broker2ID", "Broker2Order1"), Pair("broker2Date", "2020-01-01"))
      )
   }

   @Test
   fun formattedValueWithSameTypeButDifferentFormatsAreDiscoverable() {
      val schema = """
         type EventDate inherits Instant
         model Source {
            eventDate : EventDate( @format = "MM/dd/yy'T'HH:mm:ss.SSSX" )
         }
         model Target {
            eventDate : EventDate( @format = "yyyy-MM-dd'T'HH:mm:ss.SSSX" )
         }
      """.trimIndent()
      val (vyne, _) = testVyne(schema)
      vyne.addJsonModel("Source", """{ "eventDate" : "05/28/20T13:44:23.000Z" }""")
      val result = vyne.query().build("Target")
      result.isFullyResolved.should.be.`true`
      (result["Target"]!!.toRawObject() as Map<*, *>).get("eventDate").should.equal("2020-05-28T13:44:23.000Z")
   }

   @Test
   fun `when projecting, values with same type but different format get formats applied`()
   {
      val schema = """
         type EventDate inherits Instant
         model Source {
            eventDate : EventDate( @format = "MM/dd/yy'T'HH:mm:ss.SSSX" )
         }
         model Target {
            eventDate : EventDate( @format = "MM-dd-yy'T'HH:mm:ss.SSSX" )
         }
      """.trimIndent()
      val (vyne, _) = testVyne(schema)
      vyne.addJsonModel("Source", """{ "eventDate" : "05/28/20T13:44:23.000Z" }""")
      val result = vyne.query("""findOne { Source } as Target""", ResultMode.VERBOSE)
      result.isFullyResolved.should.be.`true`
      (result["Target"]!!.toRawObject() as Map<*, *>).get("eventDate").should.equal("05-28-20T13:44:23.000Z")
   }

   @Test
   fun `when projecting a collection, values with same type but different format get formats applied`()
   {
      val schema = """
         type EventDate inherits Instant
         model Source {
            eventDate : EventDate( @format = "MM/dd/yy'T'HH:mm:ss.SSSX" )
         }
         model Target {
            eventDate : EventDate( @format = "MM-dd-yy'T'HH:mm:ss.SSSX" )
         }
      """.trimIndent()
      val (vyne, _) = testVyne(schema)
      vyne.addJsonModel("Source[]", """[{ "eventDate" : "05/28/20T13:44:23.000Z" }]""")
      val result = vyne.query("""findOne { Source[] } as Target[]""", ResultMode.VERBOSE)
      result.isFullyResolved.should.be.`true`
      val map = result.resultMap["lang.taxi.Array<Target>"] as List<TypeNamedInstance>
      val firstEntry = map.first().value as Map<String,TypeNamedInstance>
      firstEntry["eventDate"]!!.value.should.equal("05-28-20T13:44:23.000Z")
   }

   @Ignore("This test throws StackOverFlowException, will be investigated.")
   @Test
   fun `should use cache for multiple invocations of given service operation`() {
      val testSchema = """
         model Client {
            name : PersonName as String
            country : CountryCode as String
         }
         model Country {
             countryCode : CountryCode
             countryName : CountryName as String
         }
         model ClientAndCountry {
            personName : PersonName
            countryName : CountryName
         }

         service MultipleInvocationService {
            @StubResponse("mockCustomers")
            operation getCustomers():Client[]

            @StubResponse("mockCountry")
            operation getCountry(CountryCode): Country
         }
      """.trimIndent()
      val stubInvocationService = StubService()

      val cacheAwareInvocationService = CacheAwareOperationInvocationDecorator(stubInvocationService)
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(cacheAwareInvocationService)
      val vyne = Vyne(queryEngineFactory).addSchema(TaxiSchema.from(testSchema))
      stubInvocationService.addResponse("mockCustomers", vyne.parseJsonModel("Client[]", """
         [
            { name : "Jimmy", country : "UK" },
            { name : "Marty", country : "UK" },
            { name : "Devrim", country : "TR" }
         ]
         """.trimIndent()))


      stubInvocationService.addResponse("mockCountry", object : StubResponseHandler {
         override fun invoke(operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            val countryCode = parameters.first().second.value!!.toString()
            return if (countryCode == "UK") {
               vyne.typedValue("Country", "United Kingdom")
            } else {
               vyne.typedValue("Country", "Turkey")
            }
         }
      })
//      val result =  vyne.query("""
//        findAll { Client } as ClientAndCountry
//      """.trimIndent())
   }

   val enumSchema = TaxiSchema.from("""
                namespace common {
                   enum BankDirection {
                     BankBuys("bankbuys"),
                     BankSell("banksell")
                   }

                   model CommonOrder {
                      direction: BankDirection
                   }
                }
                namespace BankX {
                   enum BankXDirection {
                        BUY("buy") synonym of common.BankDirection.BankBuys,
                        SELL("sell") synonym of common.BankDirection.BankSell
                   }
                   model BankOrder {
                      buySellIndicator: BankXDirection
                   }
                }

      """.trimIndent())

   @Test
   fun `should build by using synonyms`() {

      // Given
      val (vyne, stubService) = testVyne(enumSchema)
      vyne.addJsonModel(
         "BankX.BankOrder", """ { "buySellIndicator" : "BUY" } """)

      // When
      val result = vyne.query().build("common.CommonOrder")

      // Then
      val rawResult = result.results.values.first()!!.toRawObject()
      rawResult.should.equal(mapOf("direction" to "BankBuys"))

   }

   @Test
   fun `should build by using lenient synonyms`() {
      val lenientEnumSchema = TaxiSchema.from("""
                namespace common {
                   enum BankDirection {
                     BankBuys("bankbuys"),
                     BankSell("banksell")
                   }

                   model CommonOrder {
                      direction: BankDirection
                   }
                }
                namespace BankX {
                   lenient enum BankXDirection {
                        BUY("bank_buys") synonym of common.BankDirection.BankBuys,
                        SELL("bank_sells") synonym of common.BankDirection.BankSell
                   }
                   model BankOrder {
                      buySellIndicator: BankXDirection
                   }
                }

      """.trimIndent())

      // Given
      val (vyne, stubService) = testVyne(lenientEnumSchema)

      fun query(factJson: String): TypedObject {
         return vyne
            .query(
               additionalFacts = setOf(
                  vyne.parseJsonModel("BankX.BankOrder", factJson)
               ))
            .build("common.CommonOrder")
            .get("common.CommonOrder") as TypedObject
      }
      // When
      query(""" { "buySellIndicator" : "BUY" } """)["direction"].value.should.equal("BankBuys")
      query(""" { "buySellIndicator" : "buy" } """)["direction"].value.should.equal("BankBuys")
   }

   @Test
   fun `should build by using default enum values`() {
      val lenientEnumSchema = TaxiSchema.from("""
                namespace common {
                   enum BankDirection {
                     BankBuys("bankbuys"),
                     BankSell("banksells")
                   }

                   model CommonOrder {
                      direction: BankDirection
                   }
                }
                namespace BankX {
                   enum BankXDirection {
                        BUY("bank_buys") synonym of common.BankDirection.BankBuys,
                        default SELL("bank_sells") synonym of common.BankDirection.BankSell
                   }
                   model BankOrder {
                      buySellIndicator: BankXDirection
                   }
                }

      """.trimIndent())

      // Given
      val (vyne, stubService) = testVyne(lenientEnumSchema)

      fun query(factJson: String): TypedObject {
         return vyne
            .query(
               additionalFacts = setOf(
                  vyne.parseJsonModel("BankX.BankOrder", factJson)
               ))
            .build("common.CommonOrder")
            .get("common.CommonOrder") as TypedObject
      }
      // When
      query(""" { "buySellIndicator" : "BUY" } """)["direction"].value.should.equal("bankbuys")
      // Note here that buy doesn't resolve, so the default of SELL should be applied
      query(""" { "buySellIndicator" : "buy" } """)["direction"].value.should.equal("banksells")
   }

   @Test
   fun `should build by using synonyms with vyneql`() {

      // Given
      val (vyne, stubService) = testVyne(enumSchema)
      vyne.addJsonModel("BankX.BankOrder[]", """ [ { "buySellIndicator" : "BUY" }, { "buySellIndicator" : "SELL" } ] """.trimIndent())

      // When
      val result = vyne.query(""" findOne { BankOrder[] } as  CommonOrder[] """)

      // Then
      val results = result.results.values.first()!! as TypedCollection
      results.size.should.equal(2)
      results.map { it.value }.should.equals(mapOf("direction" to "BankBuys", "direction" to "BankSells"))
   }

   @Test
   fun `should build by using synonyms value and name`() {

      val (vyne, stubService) = testVyne(enumSchema)

      // Query by enum value
      val factValue = vyne.parseJsonModel("BankDirection", """ { "name": "bankbuys" } """)
      val resultValue = vyne.query(additionalFacts = setOf(factValue)).build("BankOrder")
      val rawResultValue = resultValue.results.values.first()!!.toRawObject()
      rawResultValue.should.equal(mapOf("buySellIndicator" to "buy"))

      // Query by enum name
      val factName = vyne.parseJsonModel("BankDirection", """ { "name": "BankSell" } """)
      val resultName = vyne.query(additionalFacts = setOf(factName)).build("BankOrder")
      val rawResultName = resultName.results.values.first()!!.toRawObject()
      rawResultName.should.equal(mapOf("buySellIndicator" to "SELL"))
   }

   @Test
   fun `should build by using synonyms value and name different than String`() {

      val enumSchema = TaxiSchema.from("""
                namespace common {
                   enum BankDirection {
                     BankBuys(1),
                     BankSell(2)
                   }

                   model CommonOrder {
                      direction: BankDirection
                   }
                }
                namespace BankX {
                   enum BankXDirection {
                        BUY(3) synonym of common.BankDirection.BankBuys,
                        SELL(4) synonym of common.BankDirection.BankSell
                   }
                   model BankOrder {
                      buySellIndicator: BankXDirection
                   }
                }

      """.trimIndent())

      val (vyne, stubService) = testVyne(enumSchema)
      vyne.addJsonModel(
         "BankX.BankOrder", """ { "buySellIndicator" : 3 } """)

      // When
      val result = vyne.query().build("common.CommonOrder")

      // Then
      val rawResult = result.results.values.first()!!.toRawObject()
      rawResult.should.equal(mapOf("direction" to 1))
   }


   @Test
   fun `retrieve all types that can discovered through single argument function invocations`() {
      val testSchema = """
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

   operation getClients(Client):Invoice
}
""".trimIndent()
      val stubInvocationService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubInvocationService)
      val vyne = Vyne(queryEngineFactory).addSchema(TaxiSchema.from(testSchema))
      val fqn = "vyne.example.TaxFileNumber"
      val accessibleTypes = vyne.accessibleFrom(fqn)
      accessibleTypes.should.have.size(2)
   }

   @Test
   fun `retrieve all types that can discovered through single argument function invocations in a large graph`() {
      val schemaBuilder = StringBuilder()
         .appendln("namespace vyne.example")

      val end = 1000
      val range = 0..end

      for (index in range) {
         schemaBuilder.appendln("type alias Type$index as String")
      }

      schemaBuilder.appendln("service serviceWithTooManyOperations {")
      for (index in 0 until range.last) {
         schemaBuilder.appendln("operation getType$index(Type$index): Type${index + 1}")
      }
      schemaBuilder.appendln("}")

      val stubInvocationService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubInvocationService)
      val vyne = Vyne(queryEngineFactory).addSchema(TaxiSchema.from(schemaBuilder.toString()))

      val fqn = "vyne.example.Type0"
      val accessibleTypes = vyne.accessibleFrom(fqn)
      accessibleTypes.should.have.size(end)
   }

   @Test
   fun `given multiple valid services to call with equal cost, should invoke all until a result is found`() {
      // The issue we're testing here is if there are mutliple ways to find a value, all which look the same,
      // but some which generate values, and others that don't, that we should keep trying until we find the approach
      // that works.
      val (vyne, stub) = testVyne("""
         model User {
            userId : UserId as Int
            userName : UserName as String
         }
         service Users {
            @StubResponse("lookupByIdEven")
            operation lookupByIdEven(id:UserId):User
            @StubResponse("lookupByIdOdd")
            operation lookupByIdOdd(id:UserId):User
         }
      """.trimIndent())

      stub.addResponse("lookupByIdEven") { _, parameters ->
         val (_, userId) = parameters.first()
         val userIdValue = userId.value as Int
         if (userIdValue % 2 == 0) {
            vyne.parseJsonModel("User", """{ "userId" : $userIdValue, "userName" : "Jimmy Even" }""")
         } else {
            error("Not found") // SImulate a 404
//            TypedNull(vyne.type("User"))
         }
      }
      stub.addResponse("lookupByIdOdd") { _, parameters ->
         val (_, userId) = parameters.first()
         val userIdValue = userId.value as Int
         if (userIdValue % 2 != 0) {
            vyne.parseJsonModel("User", """{ "userId" : $userIdValue, "userName" : "Jimmy Odd" }""")
         } else {
            error("not found")  // SImulate a 404
//            TypedNull(vyne.type("User"))
         }
      }
      val resultEven = vyne.query(additionalFacts = setOf(vyne.parseKeyValuePair("UserId", 2))).find("UserName")
      resultEven.isFullyResolved.should.be.`true`
      resultEven["UserName"]!!.value.should.equal("Jimmy Even")

      val resultOdd = vyne.query(additionalFacts = setOf(vyne.parseKeyValuePair("UserId", 3))).find("UserName")
      resultOdd.isFullyResolved.should.be.`true`
      resultOdd["UserName"]!!.value.should.equal("Jimmy Odd")
   }

   @Test
   fun `when projecting to a model, functions on the output model are evaluated`() {
      val (vyne,stub) = testVyne("""
         model OutputModel {
            username : String by concat(this.firstName,this.lastName)
            firstName : FirstName as String
            lastName : LastName as String
            favouriteCoffee : String by default("Latte")
         }
         model InputModel {
            firstName : FirstName
            lastName : LastName
         }
         service UserService {
            @StubResponse("findUsers")
            operation findAllUsers() : InputModel[]
         }
      """)
      val inputJson = """{ "firstName" : "Jimmy", "lastName" : "Pitt" }"""
      val user = TypedInstance.from(vyne.type("InputModel"), inputJson, vyne.schema, source = Provided)
      stub.addResponse("findUsers", TypedCollection.from(listOf(user)))
      val queryResult = vyne.query("findAll { InputModel[] } as OutputModel[]")
      val firstEntity = (queryResult["OutputModel[]"] as TypedCollection).first() as TypedObject
      firstEntity["username"].value.should.equal("JimmyPitt")
      firstEntity["favouriteCoffee"].value.should.equal("Latte")
      firstEntity["firstName"].value.should.equal("Jimmy")
      firstEntity["lastName"].value.should.equal("Pitt")
   }

   @Test
   fun `vyne should accept Instant parameters that are in ISO format`() {
      val testSchema = """
         type alias Symbol as String
         type TransactionEventDateTime inherits Instant

         type OrderWindowSummary {
            symbol : Symbol by xpath("/symbol")
            // 2019-12-03 16:07:59.7980000
            @Between
            orderDateTime : TransactionEventDateTime( @format = "yyyy-MM-dd HH:mm:ss.SSSSSSS") by xpath("/eventDate")
         }

         service CacheService {
            @StubResponse("findBetween")
            operation findByOrderDateTimeBetween(start : TransactionEventDateTime, end : TransactionEventDateTime ):
                       OrderWindowSummary[]( TransactionEventDateTime >= start, TransactionEventDateTime < end )
         }
      """.trimIndent()
      val stubInvocationService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubInvocationService)
      val vyne = Vyne(queryEngineFactory).addSchema(TaxiSchema.from(testSchema))
      stubInvocationService.addResponse("findBetween", object : StubResponseHandler {
         override fun invoke(operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>): TypedInstance {
            parameters.should.have.size(2)
            parameters[0].second.value.should.be.equal(Instant.parse("2011-12-03T10:15:30Z"))
            parameters[1].second.value.should.be.equal(Instant.parse("2021-12-03T10:15:30Z"))
            return vyne.parseJsonModel("OrderWindowSummary[]",
               """
                  [
                    {
                         "symbol": "USD",
                         "orderDateTime": "2019-12-03 13:07:59.7980000"
                     }
                  ]
               """.trimIndent())
         }
      })
      vyne.query(
         """
              findAll {
                 OrderWindowSummary[] ( TransactionEventDateTime  >= "2011-12-03T10:15:30", TransactionEventDateTime < "2021-12-03T10:15:30" )
              }
              """.trimIndent())
   }
}

fun Vyne.typedValue(typeName: String, value: Any, source: DataSource = Provided): TypedInstance {
   return TypedInstance.from(this.getType(typeName), value, this.schema, source = source)
//   return TypedValue.from(this.getType(typeName), value)
}


data class Edge(val operation: Operation)
data class Vertex(val type: Type)

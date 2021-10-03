package io.vyne.query.graph

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.should
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class AlgorithmsTest {
   private val objectMapper = jacksonObjectMapper()
   private val testSchema = """
         @Foo
         type Puid inherits String
         type TraderId inherits String
         type Pnl inherits String

         @Foo
         model Product {
            puid: Puid
         }

         model Trader {
            @Foo
            traderId: TraderId
         }

         model Order {
                 puid: Puid
                 traderId: TraderId
         }

         model Employee {
            traderId: TraderId
         }


         service MockCaskService {
               operation findSingleByPuid( id : Puid ) : Product
         }

         service TraderService {
             operation findTrader(traderId: TraderId): Trader
         }

        service EmployeeService {
             operation allEmployees(): Employee
        }

         service OrderService {
              operation `findAll`( ) : Order[]
         }
      """.trimIndent()
   private val schema = TaxiSchema.from(testSchema)

   @Test
   fun findAllTypesWithAnnotationTest() {
      val matchingTypes = Algorithms.findAllTypesWithAnnotation(schema, "Foo")
      matchingTypes.toSet().should.equal(setOf("Puid", "Product", "TraderId"))
   }

   @Test
   fun findAllFunctionsWithArgumentOrReturnValueForTypeTest() {
      Algorithms
         .findAllFunctionsWithArgumentOrReturnValueForType(schema, "Order")
         .results
         .first().should.equal(
            OperationQueryResultItem(
               operation = "OrderService",
               service = "`findAll`",
               role = OperationQueryResultItemRole.ReturnVal)
         )

      val resultsForTraderId = Algorithms
         .findAllFunctionsWithArgumentOrReturnValueForType(schema, "TraderId")
         .results
         .toSet()

      val expectedResultForTraderId = setOf(OperationQueryResultItem(
         operation = "TraderService",
         service = "findTrader",
         role = OperationQueryResultItemRole.ArgVal),
         OperationQueryResultItem(
            operation = "EmployeeService",
            service = "allEmployees",
            role = OperationQueryResultItemRole.ReturnVal),
         OperationQueryResultItem(
            operation = "OrderService",
            service = "`findAll`",
            role = OperationQueryResultItemRole.ReturnVal)
      )
      resultsForTraderId.should.equal(expectedResultForTraderId)

      Algorithms
         .findAllFunctionsWithArgumentOrReturnValueForType(schema, "Puid")
         .results
         .first().should.equal(
            OperationQueryResultItem(
               operation = "MockCaskService",
               service = "findSingleByPuid",
               role = OperationQueryResultItemRole.ArgVal)
         )
   }

   @Test
   fun `findAllFunctionsWithArgumentOrReturnValueForAnnotation`() {
      val matches = Algorithms
         .findAllFunctionsWithArgumentOrReturnValueForAnnotation(schema, "Foo")
         .toSet()

      matches.should.equal(setOf(
         OperationQueryResult(
            typeName = "Puid",
            results = listOf(OperationQueryResultItem(
               operation = "MockCaskService",
               service = "findSingleByPuid",
               role = OperationQueryResultItemRole.ArgVal),
               OperationQueryResultItem(
                  operation = "OrderService",
                  service = "`findAll`",
                  role = OperationQueryResultItemRole.ReturnVal))),
         OperationQueryResult(
            typeName = "Product",
            results = listOf(OperationQueryResultItem(
               operation = "MockCaskService",
               service = "findSingleByPuid",
               role = OperationQueryResultItemRole.ReturnVal))),
         OperationQueryResult(
            typeName = "TraderId",
            results = listOf(OperationQueryResultItem(
               operation = "TraderService",
               service = "findTrader",
               role = OperationQueryResultItemRole.ArgVal),
               OperationQueryResultItem(
                  operation = "EmployeeService",
                  service = "allEmployees",
                  role = OperationQueryResultItemRole.ReturnVal),
               OperationQueryResultItem(
                  operation = "OrderService",
                  service = "`findAll`",
                  role = OperationQueryResultItemRole.ReturnVal))
         )
      )
      )
   }

   @Test
   fun `get datasources`() {
      val datasources = Algorithms.getImmediatelyDiscoverableTypes(schema)
      datasources.size.should.equal(2)
      val actualType = datasources.map { it.fullyQualifiedName }.toSet()
      actualType.should.equal(setOf("Order", "Employee"))
   }

   @Test
   fun `discover immediate paths`() {
      val datasets = Algorithms.immediateDataSourcePaths(schema)
      datasets.size.should.equal(3)
      val productDataSetJson = objectMapper.writeValueAsString(datasets.first { dataset -> dataset.exploredType == "Product".fqn() })
      val tradeDatasetJson = objectMapper.writeValueAsString(datasets.first { dataset -> dataset.exploredType == "Trader".fqn() })
      val expectedProductDatasetJson = """
         {
           "startType": {
             "fullyQualifiedName": "Order",
             "parameters": [

             ],
             "parameterizedName": "Order",
             "name": "Order",
             "namespace": "",
             "shortDisplayName": "Order",
             "longDisplayName": "Order"
           },
           "exploredType": {
             "fullyQualifiedName": "Product",
             "parameters": [

             ],
             "parameterizedName": "Product",
             "name": "Product",
             "namespace": "",
             "shortDisplayName": "Product",
             "longDisplayName": "Product"
           },
           "path": [
             [
               {
                 "first": "START_POINT",
                 "second": "Order@-141192964"
               },
               {
                 "first": "OBJECT_NAVIGATION",
                 "second": "Order/puid"
               },
               {
                 "first": "PARAM_POPULATION",
                 "second": "param/Puid"
               },
               {
                 "first": "OPERATION_INVOCATION",
                 "second": "MockCaskService@@findSingleByPuid returns Product"
               }
             ]
           ]
         }
      """
      val expectedTradeDatasetJson = """
         {
  "startType": {
    "fullyQualifiedName": "Employee",
    "parameters": [

    ],
    "parameterizedName": "Employee",
    "name": "Employee",
    "namespace": "",
    "longDisplayName": "Employee",
    "shortDisplayName": "Employee"
  },
  "exploredType": {
    "fullyQualifiedName": "Trader",
    "parameters": [

    ],
    "parameterizedName": "Trader",
    "name": "Trader",
    "namespace": "",
    "longDisplayName": "Trader",
    "shortDisplayName": "Trader"
  },
  "path": [
    [
      {
        "first": "START_POINT",
        "second": "Employee@-1779321591"
      },
      {
        "first": "OBJECT_NAVIGATION",
        "second": "Employee/traderId"
      },
      {
        "first": "PARAM_POPULATION",
        "second": "param/TraderId"
      },
      {
        "first": "OPERATION_INVOCATION",
        "second": "TraderService@@findTrader returns Trader"
      }
    ]
  ]
}
      """.trimIndent()

      objectMapper.readTree(expectedProductDatasetJson).should.equal(objectMapper.readTree(productDataSetJson))
      objectMapper.readTree(expectedTradeDatasetJson).should.equal(objectMapper.readTree(tradeDatasetJson))
   }

   @Test
   fun `discover immediate paths for given type`() {
      val testSchemaWithTrades = testSchema +
         """
            model ProfitLoss {
              traderId: TraderId
              pnl: Pnl
            }

              service TraderService {
              operation `findAll`( ) : Trader[]
            }

            service PnlService {
               operation getPnl(traderId: TraderId): ProfitLoss
            }

         """.trimIndent()

      val datasets = Algorithms.immediateDataSourcePathsFor(TaxiSchema.from(testSchemaWithTrades), "Trader")
      datasets.size.should.equal(1)
      val pnlDatasetJson = objectMapper.writeValueAsString(datasets)
      val expectedpnlDatasetJson = """
         [
           {
             "startType": {
               "fullyQualifiedName": "Trader",
               "parameters": [

               ],
               "parameterizedName": "Trader",
               "namespace": "",
               "shortDisplayName": "Trader",
               "longDisplayName": "Trader",
               "name": "Trader"
             },
             "exploredType": {
               "fullyQualifiedName": "ProfitLoss",
               "parameters": [

               ],
               "parameterizedName": "ProfitLoss",
               "namespace": "",
               "shortDisplayName": "ProfitLoss",
               "longDisplayName": "ProfitLoss",
               "name": "ProfitLoss"
             },
             "path": [
               [
                 {
                   "first": "START_POINT",
                   "second": "Trader@-1779321591"
                 },
                 {
                   "first": "OBJECT_NAVIGATION",
                   "second": "Trader/traderId"
                 },
                 {
                   "first": "PARAM_POPULATION",
                   "second": "param/TraderId"
                 },
                 {
                   "first": "OPERATION_INVOCATION",
                   "second": "PnlService@@getPnl returns ProfitLoss"
                 }
               ]
             ]
           }
         ]
      """.trimIndent()
      objectMapper.readTree(expectedpnlDatasetJson).should.equal(objectMapper.readTree(pnlDatasetJson))
   }
}

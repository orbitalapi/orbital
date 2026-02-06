package com.orbitalhq.query.graph

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.should
import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.taxi.TaxiSchema
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

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
               serviceName = "OrderService".fqn(),
               operationDisplayName = "findAll",
            operationName = OperationNames.qualifiedName("OrderService","findAll"),
               role = OperationQueryResultItemRole.Output)
         )

      val resultsForTraderId = Algorithms
         .findAllFunctionsWithArgumentOrReturnValueForType(schema, "TraderId")
         .results
         .toSet()

      val expectedResultForTraderId = setOf(OperationQueryResultItem(
         serviceName = "TraderService".fqn(),
         operationDisplayName = "findTrader",
         operationName = OperationNames.qualifiedName("TraderService","findTrader"),
         role = OperationQueryResultItemRole.Input),
         OperationQueryResultItem(
            serviceName = "EmployeeService".fqn(),
            operationDisplayName = "allEmployees",
            operationName = OperationNames.qualifiedName("EmployeeService","allEmployees"),
            role = OperationQueryResultItemRole.Output),
         OperationQueryResultItem(
            serviceName = "OrderService".fqn(),
            operationDisplayName = "findAll",
            role = OperationQueryResultItemRole.Output,
            operationName = OperationNames.qualifiedName("OrderService","findAll")
      ))
      resultsForTraderId.should.equal(expectedResultForTraderId)

      Algorithms
         .findAllFunctionsWithArgumentOrReturnValueForType(schema, "Puid")
         .results
         .first().should.equal(
            OperationQueryResultItem(
               serviceName = "MockCaskService".fqn(),
               operationDisplayName = "findSingleByPuid",
               operationName = OperationNames.qualifiedName("MockCaskService","findSingleByPuid"),
               role = OperationQueryResultItemRole.Input)
         )
   }

   @Test
   fun `find annotated enums`() {
      val schema = TaxiSchema.from("""
         @AnnotatedA
         enum Things {
            @AnnotatedB
            One,
            Two
         }
      """.trimIndent())
      val matches = Algorithms.findAllTypesContainingAnnotation(schema, "AnnotatedA")
      matches.should.have.size(1)
      matches[0].annotatedType.should.equal("Things")
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
               serviceName = "MockCaskService".fqn(),
               operationDisplayName = "findSingleByPuid",
               operationName = OperationNames.qualifiedName("MockCaskService","findSingleByPuid"),
               role = OperationQueryResultItemRole.Input),
               OperationQueryResultItem(
                  operationDisplayName = "findAll",
                  serviceName = "OrderService".fqn(),
                  role = OperationQueryResultItemRole.Output,
                  operationName = OperationNames.qualifiedName("OrderService","findAll")))),
         OperationQueryResult(
            typeName = "Product",
            results = listOf(OperationQueryResultItem(
               serviceName = "MockCaskService".fqn(),
               operationDisplayName = "findSingleByPuid",
               operationName = OperationNames.qualifiedName("MockCaskService","findSingleByPuid"),
               role = OperationQueryResultItemRole.Output))),
         OperationQueryResult(
            typeName = "TraderId",
            results = listOf(OperationQueryResultItem(
               serviceName = "TraderService".fqn(),
               operationDisplayName = "findTrader",
               operationName = OperationNames.qualifiedName("TraderService","findTrader"),
               role = OperationQueryResultItemRole.Input),
               OperationQueryResultItem(
                  operationDisplayName = "allEmployees",
                  serviceName = "EmployeeService".fqn(),
                  role = OperationQueryResultItemRole.Output,
                  operationName = OperationNames.qualifiedName("EmployeeService","allEmployees")),
               OperationQueryResultItem(
                  operationDisplayName = "findAll",
                  serviceName = "OrderService".fqn(),
                  role = OperationQueryResultItemRole.Output,
                  operationName = OperationNames.qualifiedName("OrderService","findAll")))
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

}

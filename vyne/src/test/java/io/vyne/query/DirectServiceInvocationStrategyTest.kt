package io.vyne.query

import com.winterbe.expekt.should
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.schemas.Operation
import io.vyne.schemas.PropertyToParameterConstraint
import io.vyne.testVyne
import lang.taxi.Operator
import lang.taxi.services.operations.constraints.ConstantValueExpression
import lang.taxi.services.operations.constraints.PropertyTypeIdentifier
import lang.taxi.types.QualifiedName
import org.junit.Test
import java.time.Instant

class DirectServiceInvocationStrategyTest {
   val schema = """
      type Trade {
         id : TradeId as String
         timestamp : TradeDate as Instant
         clientId : ClientId as String
      }
      type Client {
         clientId : ClientId
      }
      type alias TradeList as Trade[]
      service TradeService {
         operation findAllClients():Client[]
         operation findAllTrades():Trade[]
         operation findClientTrades(clientId:ClientId,startDate:TradeDate,endDate:TradeDate): Trade[](
            ClientId = clientId,
            TradeDate >= startDate,
            TradeDate < endDate
         )
         operation findTradesBetween(startDate:TradeDate,endDate:TradeDate) : Trade[](
            TradeDate >= startDate,
            TradeDate < endDate
         )
      }

      type Order {
         timestamp : OrderDate as Instant
      }
      service OrderService {
         operation findAllOrders(): Order[]
      }
      """.trimIndent()


   @Test
   fun given_noInputs_then_noArgServiceIsCandidate() {
      val candidates = getCandidatesFor("Trade[]")
      candidates.should.have.size(1)
      // Note - doesn't include findAllClients
      candidates.first().name.should.equal("findAllTrades")
   }

   @Test
   fun given_noInputs_when_lookingWithTypeAliasForCollectionType_then_serviceIsFound() {
      val candidates = getCandidatesFor("TradeList")
      candidates.should.have.size(1)
      candidates.first().name.should.equal("findAllTrades")
   }

   @Test
   fun given_inputsThatAreViableForConstraints_then_serviceIsCandidate() {
      val expression = ConstrainedTypeNameQueryExpression("Trade[]", listOf(
         PropertyToParameterConstraint(
            PropertyTypeIdentifier(QualifiedName.from("TradeDate")),
            Operator.GREATER_THAN_OR_EQUAL_TO,
            ConstantValueExpression(Instant.parse("2020-05-10T10:00:00Z"))
         ),
         PropertyToParameterConstraint(
            PropertyTypeIdentifier(QualifiedName.from("TradeDate")),
            Operator.LESS_THAN,
            ConstantValueExpression(Instant.parse("2020-05-10T11:00:00Z"))
         )
      ))
      val candidates = getCandidatesFor(expression)
      candidates.should.have.size(1)
      // Note - this also asserts that findClientTrades() wasn't a candidate,
      // because we lacked the ClientId
      candidates.first().name.should.equal("findTradesBetween")
   }

   @Test
   fun given_constraintsProvideValues_then_theseAreIncludedInServiceCalls() {
      val (vyne, stub) = testVyne(schema)
      stub.addResponse("findTradesBetween", TypedInstance.from(vyne.type("Trade[]"), emptyList<Any>(), vyne.schema, source = Provided))
      vyne.query().find(ConstrainedTypeNameQueryExpression("Trade[]", listOf(
         PropertyToParameterConstraint(
            PropertyTypeIdentifier(QualifiedName.from("TradeDate")),
            Operator.GREATER_THAN_OR_EQUAL_TO,
            ConstantValueExpression(Instant.parse("2020-05-10T10:00:00Z"))
         ),
         PropertyToParameterConstraint(
            PropertyTypeIdentifier(QualifiedName.from("TradeDate")),
            Operator.LESS_THAN,
            ConstantValueExpression(Instant.parse("2020-05-10T11:00:00Z"))
         )
      )))

      val parameters = stub.invocations["findTradesBetween"] ?: emptyList()
      parameters.should.have.size(2)
      parameters[0].value.should.equal(Instant.parse("2020-05-10T10:00:00Z"))
      parameters[1].value.should.equal(Instant.parse("2020-05-10T11:00:00Z"))
   }

   @Test
   fun `findAll() operation will not be invoked for a vyneQL with constraints`() {
      val expression = ConstrainedTypeNameQueryExpression("Order[]", listOf(
         PropertyToParameterConstraint(
            PropertyTypeIdentifier(QualifiedName.from("OrderDate")),
            Operator.GREATER_THAN_OR_EQUAL_TO,
            ConstantValueExpression(Instant.parse("2020-05-10T10:00:00Z"))
         ),
         PropertyToParameterConstraint(
            PropertyTypeIdentifier(QualifiedName.from("OrderDate")),
            Operator.LESS_THAN,
            ConstantValueExpression(Instant.parse("2020-05-10T11:00:00Z"))
         )
      ))
      val candidates = getCandidatesFor(expression)
      candidates.should.be.empty
   }

   private fun getCandidatesFor(typeName: String): List<Operation> {
      return getCandidatesFor(TypeNameQueryExpression(typeName))
   }

   private fun getCandidatesFor(expression: QueryExpression): List<Operation> {
      val (vyne, stub) = testVyne(schema)
      val strategy = DirectServiceInvocationStrategy(stub.toOperationInvocationService())
      val queryContext = vyne.query()
      return strategy.getCandidateOperations(vyne.schema, queryContext.parseQuery(expression))
         .flatMap { (_, operationMap) -> operationMap.keys.toList() }
   }
}

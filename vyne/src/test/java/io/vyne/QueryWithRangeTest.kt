package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.TypedInstance
import io.vyne.query.ConstrainedTypeNameQueryExpression
import io.vyne.schemas.PropertyToParameterConstraint
import io.vyne.schemas.fqn
import lang.taxi.Operator
import lang.taxi.services.operations.constraints.ConstantValueExpression
import lang.taxi.services.operations.constraints.PropertyTypeIdentifier
import lang.taxi.types.QualifiedName
import org.junit.Test
import java.time.Instant

class QueryWithRangeTest {

   @Test
   fun givenQueryWithRangeParameters_then_callsService() {
      val schema = """
      type Trade {
         id : TradeId as String
         timestamp : TradeDate as Instant
      }
      service TradeService {
         operation findTrades(startDate:TradeDate,endDate:TradeDate) : Trade[](
            TradeDate >= startDate,
            TradeDate < endDate
         )
      }
      """.trimIndent()
      val (vyne, stub) = testVyne(schema)
      stub.addResponse("findTrades", TypedInstance.from(vyne.type("Trade[]"), emptyList<Any>(), vyne.schema))

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

      // See also DirectServiceInvocationStrategyTest
      stub.invocations["findTrades"].should.have.size(2)
   }
}

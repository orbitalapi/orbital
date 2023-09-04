package com.orbitalhq

import com.winterbe.expekt.should
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.ConstrainedTypeNameQueryExpression
import com.orbitalhq.schemas.PropertyToParameterConstraint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import lang.taxi.Operator
import lang.taxi.services.operations.constraints.ConstantValueExpression
import lang.taxi.services.operations.constraints.PropertyTypeIdentifier
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
      stub.addResponse("findTrades", TypedInstance.from(vyne.type("Trade[]"), emptyList<Any>(), vyne.schema, source = Provided))

      // We need simpler api for expressing date ranges
      runBlocking {vyne.query().find(ConstrainedTypeNameQueryExpression("Trade[]", listOf(
         PropertyToParameterConstraint(
            PropertyTypeIdentifier(vyne.type("TradeDate").taxiType),
            Operator.GREATER_THAN_OR_EQUAL_TO,
            ConstantValueExpression(Instant.parse("2020-05-10T10:00:00Z"))
         ),
         PropertyToParameterConstraint(
            PropertyTypeIdentifier(vyne.type("TradeDate").taxiType),
            Operator.LESS_THAN,
            ConstantValueExpression(Instant.parse("2020-05-10T11:00:00Z"))
         )
      ))).results
         .catch { null }
         .toList()}

      // See also DirectServiceInvocationStrategyTest
      stub.invocations["findTrades"].should.have.size(2)
   }
}


package io.vyne.demos.tradeCompliance.services

import io.vyne.tradeCompliance.Price
import io.vyne.tradeCompliance.TradeNotional
import io.vyne.tradeCompliance.TradeValue
import lang.taxi.annotations.DataType
import lang.taxi.annotations.Operation
import lang.taxi.annotations.ParameterType
import lang.taxi.annotations.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Service
class TradeValueService {

   @PostMapping("/tradeValue")
   @Operation
   fun calculateValue(@RequestBody request: TradeValueRequest) = calculateValue(request.notional, request.price)

   fun calculateValue(notional: TradeNotional, price: Price): TradeValue {
      return TradeValue(
         currency = price.currency,
         value = price.value.multiply(notional)
      )
   }
}

@ParameterType
@DataType
data class TradeValueRequest(val notional: TradeNotional, val price: Price)

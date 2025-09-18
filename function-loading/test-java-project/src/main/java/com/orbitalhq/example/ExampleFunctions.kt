package com.orbitalhq.example

import com.google.auto.service.AutoService
import com.orbitalhq.functions.TaxiFunction
import com.orbitalhq.functions.TaxiFunctionProvider
import com.orbitalhq.functions.TaxiParam

@AutoService(TaxiFunctionProvider::class)
class ExampleFunctions : TaxiFunctionProvider {

   @TaxiFunction
   fun sayHello(@TaxiParam name: String):String {
      return "Hello, $name"
   }
}

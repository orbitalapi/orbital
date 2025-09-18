package com.foo.bar

import com.orbitalhq.functions.TaxiFunction
import com.orbitalhq.functions.TaxiParam

@TaxiFunction
fun greet(@TaxiParam name: String): String = "Hello, $name"

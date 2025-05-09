package com.orbitalhq.models.functions.stdlib.math

import com.orbitalhq.models.functions.stdlib.MathIteratingFunction
import lang.taxi.types.QualifiedName
import java.math.BigDecimal


object Sum : MathIteratingFunction() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Sum.name

   override fun fold(acc: BigDecimal?, value: BigDecimal): BigDecimal {
      return acc?.add(value) ?: value
   }
}


object Max : MathIteratingFunction() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Max.name
   override fun fold(acc: BigDecimal?, value: BigDecimal): BigDecimal {
      return acc?.max(value) ?: value
   }
}

object Min : MathIteratingFunction() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Min.name
   override fun fold(acc: BigDecimal?, value: BigDecimal): BigDecimal {
      return acc?.min(value) ?: value
   }
}

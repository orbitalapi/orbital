package io.vyne.dataQuality

import io.vyne.schemas.Field
import io.vyne.schemas.Type
import lang.taxi.types.QualifiedName


interface RuleApplicabilityPredicate {
   val functionName: QualifiedName
   fun applies(type: Type, field: Field?): Boolean
}

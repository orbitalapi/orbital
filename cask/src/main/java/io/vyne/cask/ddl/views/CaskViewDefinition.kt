package io.vyne.cask.ddl.views

import lang.taxi.types.QualifiedName
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
data class CaskViewDefinition(
   val typeName: QualifiedName,
   val inherits: List<QualifiedName> = emptyList(),
   val distinct: Boolean = false,
   val join: ViewJoin,
   val whereClause: String? = null
)

data class ViewJoin(
   val kind: ViewJoinKind,
   val left: QualifiedName,
   val right: QualifiedName,
   val joinOn: List<JoinExpression>
) {
   enum class ViewJoinKind(val statement: String) {
      INNER("inner join"),
      LEFT_OUTER("left outer join"),
      RIGHT_OUTER("right outer join")
   }

   val types = listOf(left, right)
}

data class JoinExpression(
   val leftField: String,
   val rightField: String
)

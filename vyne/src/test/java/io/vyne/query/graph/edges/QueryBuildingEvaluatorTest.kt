package io.vyne.query.graph.edges

import com.winterbe.expekt.should
import io.vyne.schemas.fqn
import io.vyne.utils.withoutWhitespace
import org.junit.Test

class QueryBuildingEvaluatorTest  {
   @Test
   fun generatesSimpleFindQuery() {
      QueryBuildingEvaluator.buildQuery(
         "com.demo.Person".fqn(),
         "com.demo.people.PersonId".fqn(),
         "jimmy"
      ).withoutWhitespace().should.equal(
         """find { com.demo.Person( com.demo.people.PersonId == "jimmy" ) }""".withoutWhitespace()
      )
   }

   @Test
   fun numberParametersAreNotQuoted() {
      QueryBuildingEvaluator.buildQuery(
         "com.demo.Person".fqn(),
         "com.demo.people.PersonId".fqn(),
         1
      ).withoutWhitespace().should.equal(
         """find { com.demo.Person( com.demo.people.PersonId == 1 ) }""".withoutWhitespace()
      )
   }
}

package io.vyne.dataQuality.hub.reports

import com.winterbe.expekt.should
import io.vyne.dataQuality.*
import io.vyne.schemas.fqn
import lang.taxi.types.AttributePath
import lang.taxi.types.QualifiedName
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.time.Instant

@SpringBootTest
@RunWith(SpringRunner::class)
class DataQualityEventControllerTest {

   @Autowired
   lateinit var controller: DataQualityEventController

   @Autowired
   lateinit var repository: PersistedQualityReportEventRepository

   @Test
   fun `can insert and query some events`() {
      controller.submitQualityReportEvent(
         DataSubjectQualityReportEvent(
            DataQualitySubject.DataStore,
            "foo.bar.Person".fqn(),
            evaluationWithScores(60, 70, 80),
            "Cask foo.bar.Person",
            Instant.now()
         )
      )
      controller.submitQualityReportEvent(
         DataSubjectQualityReportEvent(
            DataQualitySubject.Message,
            "foo.bar.Person".fqn(),
            evaluationWithScores(10, 20, 30),
            "Topic /people",
            Instant.now()
         )
      )

      val averageScore =
         controller.getScore("foo.bar.Person", Instant.now().minusSeconds(10), Instant.now().plusSeconds(10))
      averageScore.averagedScoreBySubject.should.have.size(2)
      val forDataStore = averageScore.averagedScoreBySubject.first { it.subjectKind == DataQualitySubject.DataStore }
      forDataStore.recordCount.should.equal(1)
      forDataStore.subjectIdentifier.should.equal("Cask foo.bar.Person")
      forDataStore.score.should.be.closeTo(70.0)

   }

   private fun evaluationWithScores(vararg scores: Int) = AveragedDataQualityEvaluation(
      scores.map { score ->
         AttributeDataQualityRuleEvaluation(
            AttributePath.from("foo.bar.baz"), DataQualityRuleEvaluation(
               QualifiedName.from("foo.bar.someRule"), score, GradeTable.DEFAULT.grade(score)
            )
         )
      }
   )

}

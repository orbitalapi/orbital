package io.vyne.dataQuality

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.vyne.models.TypedInstance
import io.vyne.schemas.QualifiedNameAsStringDeserializer
import io.vyne.schemas.QualifiedNameAsStringSerializer
import lang.taxi.types.AttributePath
import lang.taxi.types.QualifiedName
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*
import kotlin.math.roundToInt

interface DataQualityRuleEvaluator {
   val qualifiedName: QualifiedName
   fun evaluate(instance: TypedInstance?): Mono<DataQualityRuleEvaluation>
}

data class GradeTable(val scores: List<Pair<IntRange, RuleGrade>>) {
   private val sortedScores: List<Pair<IntRange, RuleGrade>> = scores.sortedByDescending { it.first.first }
   fun grade(score: Int): RuleGrade {
      return sortedScores.first { it.first.contains(score) }.second
   }

   companion object {
      fun fromThresholds(badUpperThreshold: Int = 65, warningUpperThreshold: Int = 90): GradeTable {
         val badRange = 0..badUpperThreshold
         val warningRange = (badUpperThreshold + 1)..warningUpperThreshold
         val goodRange = (warningUpperThreshold + 1)..100
         return GradeTable(
            listOf(
               badRange to RuleGrade.BAD,
               warningRange to RuleGrade.WARNING,
               goodRange to RuleGrade.GOOD
            )
         )
      }

      val DEFAULT = fromThresholds()

   }
}

data class DataSubjectQualityReportEvent(
   val subjectKind: DataQualitySubject,
   // Using the Vyne qualified name here because we already have serializers / deserializers wired up for it.
   @JsonSerialize(using = QualifiedNameAsStringSerializer::class)
   @JsonDeserialize(using = QualifiedNameAsStringDeserializer::class)
   val subjectType: io.vyne.schemas.QualifiedName,
   // TODO : This should really be a DataQualityScore, but I need to wire up
   // polymorphic seriazlation / deserialization for that.
   val report: AveragedDataQualityEvaluation,
   /**
    * An optional identifer that a reporter may include that helps identify
    * the entity that this event relates to
    */
   val identifier: Any?,
   val timestamp: Instant,
   val eventId: String = UUID.randomUUID().toString()
)

enum class DataQualitySubject {
   Operation,
   DataStore,
   Message
}

data class AveragedDataQualityEvaluation(
   val evaluations: List<AttributeDataQualityRuleEvaluation>,
   @JsonIgnore
   private val gradeTable: GradeTable = GradeTable.DEFAULT
) : DataQualityScore {
   override val score = if (evaluations.isEmpty()) 100 else evaluations.map { it.score }.average().roundToInt()
   override val grade: RuleGrade = gradeTable.grade(score)
}

data class AttributeDataQualityRuleEvaluation(
   val path: AttributePath,
   val result: DataQualityRuleEvaluation
) : DataQualityScore by result

interface DataQualityScore {
   val score: Int
   val grade: RuleGrade
}

data class DataQualityRuleEvaluation(
   val ruleName: QualifiedName,
   override val score: Int,
   override val grade: RuleGrade
) : DataQualityScore {
   init {
      require(score in 0..100) { "Score of $score should be between 0 and 100" }
   }
}

enum class RuleGrade {
   GOOD,
   WARNING,
   BAD,
   NOT_APPLICABLE
}

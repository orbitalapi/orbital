package io.vyne.dataQuality

import io.vyne.models.TypedInstance
import lang.taxi.types.AttributePath
import lang.taxi.types.QualifiedName
import kotlin.math.roundToInt

interface DataQualityRuleEvaluator {
   val qualifiedName: QualifiedName
   fun evaluate(instance: TypedInstance?): DataQualityRuleEvaluation
}

data class GradeTable(val scores: List<Pair<IntRange, RuleGrade>>) {
   private val sortedScores:List<Pair<IntRange,RuleGrade>> = scores.sortedByDescending { it.first.first }
   fun grade(score:Int):RuleGrade {
      return sortedScores.first { it.first.contains(score) }.second
   }
   companion object {
      fun fromThresholds(badUpperThreshold:Int = 65, warningUpperThreshold:Int = 90):GradeTable {
         val badRange = 0..badUpperThreshold
         val warningRange = (badUpperThreshold + 1)..warningUpperThreshold
         val goodRange = (warningUpperThreshold + 1)..100
         return GradeTable(listOf(
            badRange to RuleGrade.BAD,
            warningRange to RuleGrade.WARNING,
            goodRange to RuleGrade.GOOD
         ))
      }
      val DEFAULT = fromThresholds()

   }
}

data class AveragedDataQualityEvaluation(
   val evaluations: List<AttributeDataQualityRuleEvaluation>,
   val gradeTable: GradeTable = GradeTable.DEFAULT
) : DataQualityScore {
   override val score = evaluations.map { it.score }.average().roundToInt()
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

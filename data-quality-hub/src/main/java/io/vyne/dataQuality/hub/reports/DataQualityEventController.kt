package io.vyne.dataQuality.hub.reports

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.dataQuality.DataQualitySubject
import io.vyne.dataQuality.DataSubjectQualityReportEvent
import io.vyne.dataQuality.GradeTable
import io.vyne.dataQuality.RuleGrade
import mu.KotlinLogging
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*
import javax.persistence.*

private val logger = KotlinLogging.logger {}

@RestController
class DataQualityEventController(
   private val repository: PersistedQualityReportEventRepository,
   private val evaluationRepository: AttributeEvaluationRepository,
   private val mapper: ObjectMapper
) {

   // Largely just for testing, not exposed via API
   fun submitQualityReportEvent(
      event: DataSubjectQualityReportEvent
   ): ResponseEntity<Map<String,String>> {
      return submitQualityReportEvent(listOf(event))
   }

   @PostMapping("/api/events")
   fun submitQualityReportEvent(
      @RequestBody events: List<DataSubjectQualityReportEvent>
   ): ResponseEntity<Map<String,String>> {
      val eventsById = events.associateBy { it.eventId }
      val persistentEvents = events.map { event ->
         // Note, we use a seperate id, rather than the eventId as the pk, to avoid attacks
         // through PK manipulation
         PersistedQualityReportEvent(
            eventId = event.eventId,
            subjectKind = event.subjectKind,
            subjectTypeName = event.subjectType.parameterizedName,
            timestamp = event.timestamp,
            subjectIdentifier = event.identifier?.toString(),
            score = event.report.score,
            grade = event.report.grade,
            reportJson = mapper.writeValueAsString(event.report)
         )
      }
      val saved = repository.saveAll(persistentEvents)
      logger.info { "Successfully saved ${saved.size} data quality events" }
      val evaluationResults = saved.flatMap { persistentEvent ->
         val originatingEvent = eventsById[persistentEvent.eventId]
            ?: error("The persisted event with id ${persistentEvent.eventId} was not in the source data provided, this shouldn't happen")
         originatingEvent.report.evaluations.map { evaluation ->
            AttributeEvaluationResult(
               evaluation.result.ruleName.fullyQualifiedName,
               evaluation.score,
               evaluation.grade,
               evaluation.path.toString(),
               originatingEvent.subjectKind,
               originatingEvent.subjectType.fullyQualifiedName,
               originatingEvent.identifier?.toString(),
               originatingEvent.timestamp,
               persistentEvent.id
            )
         }
      }


      evaluationRepository.saveAll(evaluationResults)
      logger.info { "Successfully saved ${evaluationResults.size} data evaluation results" }
      val savedIdMap = saved.map { it.eventId to it.id }.toMap()
      return ResponseEntity(savedIdMap, HttpStatus.CREATED)
   }

   @GetMapping("/api/events")
   fun getAllReports(): List<PersistedQualityReportEvent> {
      return repository.findAll()
   }

   @GetMapping("/api/events/{typeName}")
   fun getScore(
      @PathVariable("typeName") typeName: String,
      @RequestParam("from") startTime: Instant,
      @RequestParam("to") endTime: Instant
   ): QualityReport {
      val averagedScoresByDate = repository.findAverageScoreByDay(typeName, startTime, endTime)
      val averageScoreBySubject = repository.findAverageScore(typeName, startTime, endTime)
         .map { GradedAveragedScoreBySubject(it) }
      val overallScore = averageScoreBySubject
         .fold(0.0 to 0) { acc, value ->
            val (score, count) = acc
            score + (value.score * value.recordCount) to count + value.recordCount
         }.let { (score, count) -> score / count }.toBigDecimal()
         .setScale(2, RoundingMode.HALF_DOWN)

      val averageByRule = evaluationRepository.findAverageScoreByRule(typeName, startTime, endTime)
         .map { GradedRuleSummary(it) }

      return QualityReport(
         overallScore,
         GradeTable.DEFAULT.grade(overallScore.toInt()),
         averagedScoresByDate,
         averageByRule,
         averageScoreBySubject
      )

   }

   @GetMapping("/api/events/{typeName}/period/{period}")
   fun getScoreForPeriod(
      @PathVariable("typeName") typeName: String,
      @PathVariable("period") period: ReportingPeriod
   ): QualityReport {
      val (startTime, endTime) = period.toDateRange()
      return getScore(typeName, startTime, endTime)
   }
}

enum class ReportingPeriod(private val startDate: () -> LocalDate, private val endDateInclusive: () -> LocalDate) {
   Today({ LocalDate.now() }, { LocalDate.now() }),
   Yesterday({ LocalDate.now().minusDays(1) }, { java.time.LocalDate.now().minusDays(1) }),
   Last7Days({ LocalDate.now().minusDays(7) }, { java.time.LocalDate.now() }),
   Last30Days({ LocalDate.now().minusDays(30) }, { java.time.LocalDate.now() });

   fun toDateRange(): Pair<Instant, Instant> {
      return rangeOfDates(startDate(), endDateInclusive())
   }

   companion object {
      private fun rangeOfDates(start: LocalDate, endInclusive: LocalDate): Pair<Instant, Instant> {
         return start.atStartOfDay().toInstant(ZoneOffset.UTC) to
            endInclusive.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
      }
   }


}

@Entity
data class PersistedQualityReportEvent(
   @Id
   val id: String = UUID.randomUUID().toString(),
   @Enumerated(EnumType.ORDINAL)
   val subjectKind: DataQualitySubject,
   val subjectTypeName: String,
   val timestamp: Instant,
   val subjectIdentifier: String?,
   val score: Int,
   @Enumerated(EnumType.ORDINAL)
   val grade: RuleGrade,
   @Lob
   val reportJson: String,
   val eventId: String
)


interface PersistedQualityReportEventRepository : JpaRepository<PersistedQualityReportEvent, String> {
   @Query(
      """select e.subjectTypeName as subjectTypeName,
         e.subjectKind as subjectKind,
         AVG(e.score) as score, count(*) as recordCount
         from PersistedQualityReportEvent e
         where e.timestamp >= :fromDate and e.timestamp <= :toDate and e.subjectTypeName = :subjectTypeName
         group by e.subjectKind, e.subjectTypeName
      """
   )
   fun findAverageScore(
      @Param("subjectTypeName") subjectTypeName: String,
      @Param("fromDate") fromDate: Instant,
      @Param("toDate") toDate: Instant
   ): List<AveragedScoreBySubject>


   @Query(
      """select
         cast(e.timestamp as date) as date,
         AVG(e.score) as score,
         count(*) as recordCount
         from PersistedQualityReportEvent e
         where e.timestamp >= :fromDate and e.timestamp <= :toDate and e.subjectTypeName = :subjectTypeName
         group by cast(e.timestamp as date)
      """
   )
   fun findAverageScoreByDay(
      @Param("subjectTypeName") subjectTypeName: String,
      @Param("fromDate") fromDate: Instant,
      @Param("toDate") toDate: Instant
   ): List<AveragedScoreByDate>
}

interface AveragedScoreByDate {
   val date: LocalDate
   val score: Double
   val recordCount: Int
}

interface AveragedScoreBySubject {
   val subjectTypeName: String
   val subjectKind: DataQualitySubject
   val score: Double
   val recordCount: Int
}

/**
 * Wrapper around AveragedScoreBySubject to also provide the Grade, since
 * this can't be exposed at the db layer.
 */
data class GradedAveragedScoreBySubject(
   private val scoreBySubject: AveragedScoreBySubject
) : AveragedScoreBySubject by scoreBySubject {
   val grade: RuleGrade = GradeTable.DEFAULT.grade(scoreBySubject.score.toInt())
}

data class GradedRuleSummary(
   private val ruleSummary: QualityRuleSummary
) : QualityRuleSummary by ruleSummary {
   val grade: RuleGrade = GradeTable.DEFAULT.grade(ruleSummary.score.toInt())
   override val score: BigDecimal = ruleSummary.score.setScale(1, RoundingMode.HALF_EVEN)
}

interface QualityRuleSummary {
   val ruleName: String
   val recordCount: Int
   val score: BigDecimal
}

interface AttributeEvaluationRepository : JpaRepository<AttributeEvaluationResult, String> {
   @Query(
      """select
         e.ruleName as ruleName,
         AVG(e.score) as score,
         count(*) as recordCount
         from AttributeEvaluationResult e
         where e.timestamp >= :fromDate and e.timestamp <= :toDate and e.subjectTypeName = :subjectTypeName
         group by cast(e.timestamp as date)
      """
   )
   fun findAverageScoreByRule(
      @Param("subjectTypeName") subjectTypeName: String,
      @Param("fromDate") fromDate: Instant,
      @Param("toDate") toDate: Instant
   ): List<QualityRuleSummary>
}

data class QualityReport(
   val overallScore: BigDecimal,
   val overallGrade: RuleGrade,
   val averagedScoreByDate: List<AveragedScoreByDate>,
   val ruleSummaries: List<QualityRuleSummary>,
   val averagedScoreBySubject: List<AveragedScoreBySubject>
)


@Entity
@Table(
   indexes = [Index(name = "ix_evalResult_reportId", columnList = "reportId", unique = false)]
)
data class AttributeEvaluationResult(
   val ruleName: String,
   val score: Int,
   @Enumerated(EnumType.STRING)
   val grade: RuleGrade,
   val path: String,
   val subjectKind: DataQualitySubject,
   val subjectTypeName: String,
   val identifier: String?,
   val timestamp: Instant,
   val reportId: String,
   @Id
   val id: String = UUID.randomUUID().toString()
)

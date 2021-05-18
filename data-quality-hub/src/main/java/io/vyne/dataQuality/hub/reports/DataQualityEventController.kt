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
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*
import javax.persistence.*
import kotlin.math.roundToInt

private val logger = KotlinLogging.logger {}

@RestController
class DataQualityEventController(
   private val repository: PersistedQualityReportEventRepository,
   private val mapper: ObjectMapper
) {

   @PostMapping("/api/events")
   fun submitQualityReportEvent(
      @RequestBody event: DataSubjectQualityReportEvent
   ): ResponseEntity<String> {
      val saved = repository.save(
         PersistedQualityReportEvent(
            subjectKind = event.subjectKind,
            subjectTypeName = event.subjectType.parameterizedName,
            timestamp = event.timestamp,
            subjectIdentifier = event.identifier?.toString(),
            score = event.report.score,
            grade = event.report.grade,
            reportJson = mapper.writeValueAsString(event.report)
         )
      )

      logger.info { "Created report event ${saved.id} for type ${saved.subjectTypeName} with score ${saved.score}" }
      return ResponseEntity(saved.id, HttpStatus.CREATED)
   }

   @GetMapping("/api/events/{typeName}")
   fun getScore(
      @PathVariable("typeName") typeName: String,
      @RequestParam("from") startTime: Instant,
      @RequestParam("to") endTime: Instant
   ): List<AveragedScoreBySubject> {
      val byDay = repository.findAverageScoreByDay(typeName, startTime, endTime)
      return repository.findAverageScore(typeName, startTime, endTime)
   }

   @GetMapping("/api/events/{typeName}/period/{period}")
   fun getScoreForPeriod(
      @PathVariable("typeName") typeName: String,
      @PathVariable("period") period: ReportingPeriod
   ): List<AveragedScoreBySubject> {
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
   val reportJson: String
)


interface PersistedQualityReportEventRepository : JpaRepository<PersistedQualityReportEvent, String> {
   @Query(
      """select e.subjectTypeName as subjectTypeName,
         e.subjectIdentifier as subjectIdentifier,
         e.subjectKind as subjectKind,
         AVG(e.score) as score, count(*) as recordCount
         from PersistedQualityReportEvent e
         where e.timestamp >= :fromDate and e.timestamp <= :toDate and e.subjectTypeName = :subjectTypeName
         group by e.subjectKind, e.subjectTypeName, e.subjectIdentifier
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
   val subjectIdentifier: String?
   val subjectKind: DataQualitySubject
   val score: Double
   val recordCount: Int

   val grade: RuleGrade
      get() {
         return GradeTable.DEFAULT.grade(score.roundToInt())
      }
}

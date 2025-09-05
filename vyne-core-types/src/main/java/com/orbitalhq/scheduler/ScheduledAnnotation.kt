package com.orbitalhq.scheduler

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.right
import com.orbitalhq.VyneTypes
import com.orbitalhq.schemas.fqn
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.Annotation
import lang.taxi.types.EnumValue
import lang.taxi.types.annotation

object ScheduledAnnotation {

   val ScheduledTypeName = "${VyneTypes.NAMESPACE}.scheduler.Scheduled".fqn()

   val taxi = """
namespace com.orbitalhq.scheduler

annotation Scheduled {
  /**
   * Cron expression (Quartz format)
   */
  cron: String?

  /**
   * Run with fixed rate (ISO-8601 duration or shorthand like "10m")
   */
  fixedRate: String?

  /**
   * Max number of executions (-1 = forever)
   */
  repeatCount: Int = -1

  /**
   * What to do if a fire is missed
   */
  misfirePolicy: MisfirePolicy = MisfirePolicy.Default

  /**
   * Trigger priority (higher runs first)
   */
  priority: Int = 5
}

enum MisfirePolicy {
  Default,
  FireImmediately,
  Skip
}
   """.trimIndent()
}


data class ScheduleConfiguration(
   val cron: String?,
   val fixedRate: String?,
   val repeatCount: Int = -1,
   val misfirePolicy: MisfirePolicy = MisfirePolicy.Default,
   val priority: Int = 5
) {
   enum class MisfirePolicy {
      Default,
      FireImmediately,
      Skip;
   }


   companion object {
      fun fromAnnotation(annotation: Annotation): Either<String, ScheduleConfiguration> {
         val cron = annotation.parameter("cron") as String?
         val fixedRate = annotation.parameter("fixedRate") as String?
         val repeatCount = annotation.parameter("repeatCount") as Int? ?: -1
         val misfirePolicy = (annotation.parameter("misfirePolicy") as EnumValue?)?.let {

            MisfirePolicy.valueOf(it.value.toString())
         } ?: MisfirePolicy.Default
         val parameter = annotation.parameter("priority") as Int? ?: 5
         return ScheduleConfiguration(
            cron, fixedRate, repeatCount, misfirePolicy, parameter
         ).right()
      }

      fun fromQuery(query: TaxiQlQuery): ScheduleConfiguration? {
         val config = query.annotation(ScheduledAnnotation.ScheduledTypeName.parameterizedName)
            ?.let { fromAnnotation(it).getOrElse { null } }
         return config


      }
   }
}

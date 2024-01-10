package com.orbitalhq.models.functions.stdlib.dates

import com.orbitalhq.firstRawObject
import com.orbitalhq.testVyne
import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

class DateMathTests {

   @Test
   fun addsMinutesToDateTime(): Unit = runBlocking {
      val (vyne, _) = testVyne("")
      val result = vyne
         .query(
            """given { d : DateTime = parseDate('2023-05-10T00:00:00') }
            |find { result : DateTime = addMinutes(d,60) }""".trimMargin()
         )
         .firstRawObject()
      result["result"].shouldBe("2023-05-10T01:00:00.000")
   }

   @Test
   fun subtractMinutesToDateTime(): Unit = runBlocking {
      val (vyne, _) = testVyne("")
      val result = vyne
         .query(
            """given { d : DateTime = parseDate('2023-05-10T02:00:00') }
            |find { result : DateTime = addMinutes(d,-60) }""".trimMargin()
         )
         .firstRawObject()
      result["result"].shouldBe("2023-05-10T01:00:00.000")
   }


   @Test
   fun addsMinutesToInstant(): Unit = runBlocking {
      val (vyne, _) = testVyne("")
      val result = vyne
         .query(
            """given { d : Instant = parseDate('2023-05-10T00:00:00Z') }
            |find { result : Instant = addMinutes(d,60) }""".trimMargin()
         )
         .firstRawObject()
      result["result"].shouldBe("2023-05-10T01:00:00.000Z")
   }

   @Test
   fun subtractMinutesToInstant(): Unit = runBlocking {
      val (vyne, _) = testVyne("")
      val result = vyne
         .query(
            """given { d : Instant = parseDate('2023-05-10T02:00:00') }
            |find { result : Instant = addMinutes(d,-60) }""".trimMargin()
         )
         .firstRawObject()
      result["result"].shouldBe("2023-05-10T01:00:00.000Z")
   }

   ///
   @Test
   fun addsDaysToDateTime(): Unit = runBlocking {
      val (vyne, _) = testVyne("")
      val result = vyne
         .query(
            """given { d : DateTime = parseDate('2023-05-10T00:00:00') }
            |find { result : DateTime = addDays(d,10) }""".trimMargin()
         )
         .firstRawObject()
      result["result"].shouldBe("2023-05-20T00:00:00.000")
   }

   @Test
   fun subtractDaysToDateTime(): Unit = runBlocking {
      val (vyne, _) = testVyne("")
      val result = vyne
         .query(
            """given { d : DateTime = parseDate('2023-05-10T00:00:00') }
            |find { result : DateTime = addDays(d,-5) }""".trimMargin()
         )
         .firstRawObject()
      result["result"].shouldBe("2023-05-05T00:00:00.000")
   }

   @Test
   fun addsDaysToInstant(): Unit = runBlocking {
      val (vyne, _) = testVyne("")
      val result = vyne
         .query(
            """given { d : Instant = parseDate('2023-05-10T00:00:00Z') }
            |find { result : Instant = addDays(d,10) }""".trimMargin()
         )
         .firstRawObject()
      result["result"].shouldBe("2023-05-20T00:00:00.000Z")
   }

   @Test
   fun subtractDaysToInstant(): Unit = runBlocking {
      val (vyne, _) = testVyne("")
      val result = vyne
         .query(
            """given { d : Instant = parseDate('2023-05-10T00:00:00Z') }
            |find { result : Instant = addDays(d,-5) }""".trimMargin()
         )
         .firstRawObject()
      result["result"].shouldBe("2023-05-05T00:00:00.000Z")
   }

}

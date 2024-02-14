package com.orbitalhq.models.functions.stdlib.dates

import com.orbitalhq.firstTypedObject
import com.orbitalhq.models.TypedValue
import com.orbitalhq.testVyne
import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

class NowTest {

   @Test
   fun `can get now as instant`():Unit = runBlocking {
      val (vyne, _) = testVyne("")
      val result = vyne
         .query("""find { result : Instant = now() }"""
         )
         .firstTypedObject()
      result.shouldNotBeNull()
      result["result"].shouldNotBeNull()
      result["result"].shouldBeInstanceOf<TypedValue>()
      result["result"]!!.type.name.fullyQualifiedName.shouldBe("lang.taxi.Instant")
   }

   @Test
   fun `can get now as dateTime`():Unit = runBlocking {
      val (vyne, _) = testVyne("")
      val result = vyne
         .query("""find { result : DateTime = currentDateTime() }"""
         )
         .firstTypedObject()
      result.shouldNotBeNull()
      result["result"].shouldNotBeNull()
      result["result"].shouldBeInstanceOf<TypedValue>()
      result["result"]!!.type.name.fullyQualifiedName.shouldBe("lang.taxi.DateTime")
   }

   @Test
   fun `can get now as date`():Unit = runBlocking {
      val (vyne, _) = testVyne("")
      val result = vyne
         .query("""find { result : Date = currentDate() }"""
         )
         .firstTypedObject()
      result.shouldNotBeNull()
      result["result"].shouldNotBeNull()
      result["result"].shouldBeInstanceOf<TypedValue>()
      result["result"]!!.type.name.fullyQualifiedName.shouldBe("lang.taxi.Date")
   }

   @Test
   fun `can get now as time`():Unit = runBlocking {
      val (vyne, _) = testVyne("")
      val result = vyne
         .query("""find { result : Time = currentTime() }"""
         )
         .firstTypedObject()
      result.shouldNotBeNull()
      result["result"].shouldNotBeNull()
      result["result"].shouldBeInstanceOf<TypedValue>()
      result["result"]!!.type.name.fullyQualifiedName.shouldBe("lang.taxi.Time")
   }
}

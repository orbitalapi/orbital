package com.orbitalhq.models.functions.stdlib.dates

import com.orbitalhq.firstRawObject
import com.orbitalhq.firstTypedObject
import com.orbitalhq.models.TypedValue
import com.orbitalhq.testVyne
import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf
import org.junit.Test
import java.time.LocalDateTime

class ParseDateTest {
   @Test
   fun parseDateTimeTest(): Unit = runBlocking {
      val (vyne, _) = testVyne("""""")
      val result = vyne
         .query(
            """given { d : DateTime = parseDate('2023-05-10T00:00:00') }
            find { result : DateTime }"""
         )
         .firstTypedObject()
      val typedValue = result["result"].shouldBeInstanceOf<TypedValue>()
      typedValue.type.fullyQualifiedName.shouldBe("lang.taxi.DateTime")
      typedValue.value.shouldBe(LocalDateTime.parse("2023-05-10T00:00:00"))
   }

}

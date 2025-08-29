package com.orbitalhq.functions.stdlib.collections

import com.orbitalhq.firstRawValue
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

class CollectionContainsAllOrAnyTest : DescribeSpec({
   describe("stdlib collection contains all") {
      it("should return false when the collection contains some") {
         containsAll(
            collectionToSearch = """[ "A", "B" ]""",
            requiredValues = """[ "A", "B", "C" ]"""
         ).shouldNotBeNull().shouldBeFalse()
      }
      it("should return false when the collection contains none") {
         containsAll(
            collectionToSearch = """[ "A", "B" ]""",
            requiredValues = """[ "C", "D" ]"""
         ).shouldNotBeNull().shouldBeFalse()
      }
      it("should return null when the searched collection is null") {
         containsAll(
            collectionToSearch = """null""",
            requiredValues = """[ "A", "B", "C" ]"""
         ).shouldBeNull()
      }
      it("should return false when the searched collection is empty") {
         containsAll(
            collectionToSearch = """[]""",
            requiredValues = """[ "A", "B", "C" ]"""
         ).shouldNotBeNull().shouldBeFalse()
      }
      it("should return true when the list of values is empty") {
         containsAll(
            collectionToSearch = """[ "A" , "B" ]""",
            requiredValues = """[]"""
         ).shouldNotBeNull().shouldBeTrue()
      }
      it("should return null when the list of values is null") {
         containsAll(
            collectionToSearch = """[ "A" , "B" ]""",
            requiredValues = """null"""
         ).shouldBeNull()
      }
      it("should return true when the list of values is all present") {
         containsAll(
            collectionToSearch = """[ "A" , "B" ]""",
            requiredValues = """[ "A", "B" ]"""
         ).shouldNotBeNull().shouldBeTrue()
      }
   }


   describe("stdlib collection contains any") {
      it("should return true when the collection contains some") {
         containsAny(
            collectionToSearch = """[ "A", "B" ]""",
            requiredValues = """[ "A", "B", "C" ]"""
         ).shouldNotBeNull().shouldBeTrue()
      }
      it("should return false when the collection contains none") {
         containsAny(
            collectionToSearch = """[ "A", "B" ]""",
            requiredValues = """[ "C", "D" ]"""
         ).shouldNotBeNull().shouldBeFalse()
      }
      it("should return null when the searched collection is null") {
         containsAny(
            collectionToSearch = """null""",
            requiredValues = """[ "A", "B", "C" ]"""
         ).shouldBeNull()
      }
      it("should return false when the searched collection is empty") {
         containsAny(
            collectionToSearch = """[]""",
            requiredValues = """[ "A", "B", "C" ]"""
         ).shouldNotBeNull().shouldBeFalse()
      }
      it("should return true when the list of values is empty") {
         containsAny(
            collectionToSearch = """[ "A" , "B" ]""",
            requiredValues = """[]"""
         ).shouldNotBeNull().shouldBeTrue()
      }
      it("should return null when the list of values is null") {
         containsAny(
            collectionToSearch = """[ "A" , "B" ]""",
            requiredValues = """null"""
         ).shouldBeNull()
      }
      it("should return true when the list of values is all present") {
         containsAny(
            collectionToSearch = """[ "A" , "B" ]""",
            requiredValues = """[ "A", "B" ]"""
         ).shouldNotBeNull().shouldBeTrue()
      }
   }
})

suspend fun containsAny(collectionToSearch: String, requiredValues: String):Boolean? {
   val (vyne) = testVyne("")
   return vyne.query("""
         given {
            collectionToSearch: String[] = $collectionToSearch,
            requiredValues: String[] = $requiredValues
         } find { collectionToSearch.containsAny(requiredValues) }
      """.trimIndent())
      .firstRawValue() as Boolean?
}


suspend fun containsAll(collectionToSearch: String, requiredValues: String):Boolean? {
   val (vyne) = testVyne("")
      return vyne.query("""
         given {
            collectionToSearch: String[] = $collectionToSearch,
            requiredValues: String[] = $requiredValues
         } find { collectionToSearch.containsAll(requiredValues) }
      """.trimIndent())
         .firstRawValue() as Boolean?
}

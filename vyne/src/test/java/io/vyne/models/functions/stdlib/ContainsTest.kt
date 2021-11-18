package io.vyne.models.functions.stdlib

import com.winterbe.expekt.should
import io.vyne.models.json.parseJson
import io.vyne.testVyne
import org.junit.Test

class ContainsTest {

   @Test
   fun `returns true when collection contains string`() {
      val (vyne,stub) = testVyne("""
         type PersonName inherits String
         model Person {
            name : PersonName
            friends : PersonName[]
            friendsWithSelf : Boolean by contains(this.friends, this.name)
         }
      """.trimIndent())
      val parsed = vyne.parseJson("Person[]", """[
         {   "name" : "Jack" , "friends" : ["Jack","Jill"] },
         {   "name" : "Jill" , "friends" : ["Jack"] }
         ]
      """.trimIndent()).toRawObject() as List<Map<String,Any>>
      parsed.single { it["name"] == "Jack" }.get("friendsWithSelf").should.equal(true)
      parsed.single { it["name"] == "Jill" }.get("friendsWithSelf").should.equal(false)
   }


   @Test
   fun `returns true when collection contains enum`() {
      val (vyne,stub) = testVyne("""
         type PersonName inherits String
         enum Country {
            NZ,
            AUS
         }
         model Person {
            name : PersonName
            visited : Country[]
            hasBeenToNZ : Boolean by contains(this.visited, 'NZ')
         }
      """.trimIndent())
      val parsed = vyne.parseJson("Person[]", """[
         {   "name" : "Jack" , "visited" : ["NZ","AUS"] },
         {   "name" : "Jill" , "visited" : ["AUS"] }
         ]
      """.trimIndent()).toRawObject() as List<Map<String,Any>>
      parsed.single { it["name"] == "Jack" }.get("hasBeenToNZ").should.equal(true)
      parsed.single { it["name"] == "Jill" }.get("hasBeenToNZ").should.equal(false)
   }
}

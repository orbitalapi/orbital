package com.orbitalhq

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.types.shouldBeInstanceOf
import lang.taxi.asA
import lang.taxi.compiledWithQuery
import lang.taxi.types.ArrayType
import lang.taxi.types.ObjectType

class ProjectingGrammarSpec :  DescribeSpec({
   describe("edge cases around projection grammar") {
      it("should parse untyped projection correctly") {
         val (schema, query) = """model Film {
   id : FilmId inherits Int
}

model Studio {
   id : StudioId inherits Int
}

model Actor {
   name : PersonName inherits String
}
model CastResponse {
   actors : Actor[]
}
""".compiledWithQuery("""
 find { Studio } as {
    studioId : StudioId
    film : Film as { // <--- field is an untyped projection
       id : FilmId
       cast : CastResponse as Actor[] as (actor:Actor) -> { // <--- field is an untyped projection
          personName : PersonName
       }[]
    }
 }
""".trimIndent())
         val filmType = query.projectedObjectType!!
            .field("film")
            .type.asA<ObjectType>()
         filmType.anonymous.shouldBeTrue()
         filmType.fields.shouldHaveSize(2)
         val castFieldType = filmType.field("cast")
            .type
         castFieldType.shouldBeInstanceOf<ArrayType>()
            .type.anonymous.shouldBeTrue()
      }
   }
})

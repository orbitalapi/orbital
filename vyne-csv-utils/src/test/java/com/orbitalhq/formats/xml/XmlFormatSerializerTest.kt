package com.orbitalhq.formats.xml

import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.winterbe.expekt.should
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.math.exp

class XmlFormatSerializerTest : DescribeSpec({
   describe("serializing to xml using XmlFormatSpec") {
      val schema = TaxiSchema.from(
         """
            @com.orbitalhq.formats.Xml
            model Movie {
                actors : Actor[]
                title : MovieTitle inherits String
            }

            model Actor inherits Person {
               @lang.taxi.xml.XmlAttribute
                id : ActorId inherits Int
                agent : Person
                fullName : FullName inherits String = FirstName + ' ' + LastName
            }
            model Person {
                firstName : FirstName inherits String
                lastName: LastName inherits String
            }
         """.trimIndent()
      )
      it("should write simple object to xml") {
         val typedInstance = TypedInstance.from(schema.type("Person"),
            mapOf("firstName" to "Jimmy", "lastName" to "Smith"),
            schema
         )
         val xml = XmlFormatSpec.serializer.write(typedInstance, mock {  }, schema) as String
         val expected = """<?xml version='1.0' encoding='UTF-8'?><Person><firstName>Jimmy</firstName><lastName>Smith</lastName></Person>"""
         xml.shouldBe(expected)
      }
      it("should write simple object with attribute to xml") {
         val typedInstance = TypedInstance.from(schema.type("Actor"),
            mapOf("firstName" to "Jimmy", "lastName" to "Smith", "id" to 3),
            schema
         )
         val xml = XmlFormatSpec.serializer.write(typedInstance, mock {  }, schema) as String
         val expected = """<?xml version='1.0' encoding='UTF-8'?><Actor id="3"><firstName>Jimmy</firstName><lastName>Smith</lastName><fullName>Jimmy Smith</fullName></Actor>"""
         xml.shouldBe(expected)
      }
      it("should serialize to xml") {

         val actual = mapOf(
            "actors" to listOf(
               mapOf(
                  "id" to 1,
                  "firstName" to "Mel",
                  "lastName" to "Gibson",
                  "agent" to mapOf(
                     "firstName" to "Johnny",
                     "lastName" to "Cashpott",
                  ),
                  "fullName" to "Mel Gibson"
               ),
               mapOf(
                  "id" to 2,
                  "firstName" to "Jack",
                  "lastName" to "Spratt",
                  "agent" to mapOf(
                     "firstName" to "Johnny",
                     "lastName" to "Cashpott",
                  ),
                  "fullName" to "Jack Spratt"
               ),

               ),
            "title" to "Star Wars"
         )
         val typedInstance = TypedInstance.from(schema.type("Movie"), actual, schema)
         val xml = XmlFormatSpec.serializer.write(typedInstance, mock {  }, schema) as String
         val expected = """<?xml version='1.0' encoding='UTF-8'?><Movie><actors id="1"><firstName>Mel</firstName><lastName>Gibson</lastName><agent><firstName>Johnny</firstName><lastName>Cashpott</lastName></agent><fullName>Mel Gibson</fullName></actors><actors id="2"><firstName>Jack</firstName><lastName>Spratt</lastName><agent><firstName>Johnny</firstName><lastName>Cashpott</lastName></agent><fullName>Jack Spratt</fullName></actors><title>Star Wars</title></Movie>"""

         xml.shouldBe(expected)
      }

      it("should parse a top level list") {
         val schema = TaxiSchema.from(
            """
@com.orbitalhq.formats.Xml
   model Film {
      id : FilmId inherits Int
      title : Title inherits String
      yearReleased : Released inherits Int
   }
""".trimIndent()
         )
         val xml = """<List>
    <item id="0">
        <title>ACADEMY DINOSAUR</title>
        <yearReleased>1978</yearReleased>
    </item>
    <item id="1">
        <title>ACE GOLDFINGER</title>
        <yearReleased>2019</yearReleased>
    </item>
</List>
"""
         val parsed = TypedInstance.from(schema.type("Film[]"), xml, schema, formatSpecs = listOf(XmlFormatSpec))
         parsed.shouldBeInstanceOf<TypedCollection>()
         parsed.toRawObject().shouldBe(
            listOf(
               mapOf(
                  "id" to 0,
                  "title" to "ACADEMY DINOSAUR",
                  "yearReleased" to 1978
               ),
               mapOf(
                  "id" to 1,
                  "title" to "ACE GOLDFINGER",
                  "yearReleased" to 2019
               )
            )
         )
      }

      it("should support xpath declarations and functions") {
         val schema = TaxiSchema.from(
            """
         model Foo {
            assetClass : String by xpath("/Foo/assetClass")
            identifierValue : String? by when (this.assetClass) {
               "FXD" -> left(xpath("/Foo/symbol"),6)
               else -> xpath("/Foo/isin")
            }
         }
         """.trimIndent()
         )

         fun xml(assetClass: String) = """<Foo>
         |<assetClass>$assetClass</assetClass>
         |<symbol>GBPUSD-100293</symbol>
         |<isin>ISIN-138443</isin>
         |</Foo>
      """.trimMargin()

         val fooWithSymbol = TypedInstance.from(
            schema.type("Foo"),
            xml("FXD"),
            schema,
            source = Provided,
            formatSpecs = listOf(XmlFormatSpec)
         ) as TypedObject
         fooWithSymbol["identifierValue"].value.should.equal("GBPUSD")

         val fooWithIsin = TypedInstance.from(
            schema.type("Foo"),
            xml("xxx"),
            schema,
            source = Provided,
            formatSpecs = listOf(XmlFormatSpec)
         ) as TypedObject
         fooWithIsin["identifierValue"].value.should.equal("ISIN-138443")
      }
   }

})

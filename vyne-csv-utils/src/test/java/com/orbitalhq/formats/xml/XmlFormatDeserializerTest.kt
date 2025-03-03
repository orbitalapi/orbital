package com.orbitalhq.formats.xml

import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.winterbe.expekt.should
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class XmlFormatDeserializerTest : DescribeSpec({
   describe("parsing xml using XmlFormatSpec") {
      it("should parse an XML") {
         val schema = TaxiSchema.from(
            """
            @com.orbitalhq.formats.Xml
            model Movie {
                actors : Actor[]
                title : MovieTitle inherits String
            }

            model Actor inherits Person {
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
         val xml = """
<movie>
    <actors id="1">
        <firstName>Mel</firstName>
        <lastName>Gibson</lastName>
        <agent>
            <firstName>Johnny</firstName>
            <lastName>Cashpott</lastName>
        </agent>
    </actors>
    <actors id="2">
        <firstName>Jack</firstName>
        <lastName>Spratt</lastName>
        <agent>
            <firstName>Johnny</firstName>
            <lastName>Cashpott</lastName>
        </agent>
    </actors>
    <title>Star Wars</title>
</movie>
         """.trimIndent()

         val parsed = TypedInstance.from(schema.type("Movie"), xml, schema, formatSpecs = listOf(XmlFormatSpec))
         val expected = mapOf(
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
         parsed.toRawObject().shouldBe(
            expected
         )
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
               @com.orbitalhq.formats.Xml
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
      /**
       * <row id='36471808' xml:space='preserve'>
       * <c1>12707077</c1>
       * <c2>3115</c2>
       * <c3>AT1-36471808</c3>
       * <c5>SH-36471808</c5>
       * <c7>TR</c7>
       * <c8>GBP</c8>
       * <c9>1</c9>
       * <c10>4315.02</c10>
       * <c11>827</c11>
       * <c20 m='31'>GB34MYMB23058036471808</c20>
       * <c20 m='161'>NA</c20>
       * <c20 m='174'>
       * </row>
       */
      it("is possible to embed xml within json") {
         val schema = TaxiSchema.from(
            """
type TableId inherits String
type AccountId inherits String
type CustomerId inherits String
type AccountName inherits String
type CustomerName inherits String

model CustomerMessage {
   table: TableId
   XMLRecord: Customer
}

@com.orbitalhq.formats.Xml
model Customer {
    id : CustomerId
    c0 : CustomerName?
}

model AccountMessage {
    table : TableId
    XMLRecord : Account
}

@com.orbitalhq.formats.Xml
model Account {
    id : AccountId
    c0 : AccountName?
}


"""
         )

         val src = """{
    "table" : "123",
    "XMLRecord" : "<row id=\"12707077\" xml:space=\"preserve\"><c0>123456</c0></row>"
}"""
         val typedInstance = TypedInstance.from(
            schema.type("AccountMessage"), src, schema,
            source = Provided,
            formatSpecs = listOf(XmlFormatSpec)
         )
         typedInstance.toRawObject().shouldBe(
            mapOf(
               "table" to "123",
               "XMLRecord" to mapOf("id" to "12707077", "c0" to "123456")
            ),

            )
      }

      it("is possible to use complex expressions on xml embedded in json") {
         val schema = TaxiSchema.from(
            """
               model MyMessage {
                   messageId : MessageId inherits String
                   xmlRecord : Foo
               }

               @com.orbitalhq.formats.Xml
         model Foo {
            assetClass : String by xpath("/Foo/assetClass")
            identifierValue : String? by when (this.assetClass) {
               "FXD" -> left(xpath("/Foo/symbol"),6)
               else -> xpath("/Foo/isin")
            }
         }
         """.trimIndent()
         )

         fun xml(assetClass: String) =
            """<Foo><assetClass>$assetClass</assetClass><symbol>GBPUSD-100293</symbol><isin>ISIN-138443</isin></Foo>"""

         val json = """
            { "messageId" : "123" , "xmlRecord" : "${xml("FXD")}" }
         """.trimIndent()
         val typedInstance = TypedInstance.from(
            schema.type("MyMessage"), json, schema,
            source = Provided,
            formatSpecs = listOf(XmlFormatSpec)
         )
         typedInstance.toRawObject().shouldBe(
            mapOf(
               "messageId" to "123",
               "xmlRecord" to mapOf("assetClass" to "FXD", "identifierValue" to "GBPUSD")
            ),
         )
      }

      // GH-16
      it("deserializes single-element collections correctly") {
         val schema = TaxiSchema.from(
            """
                @com.orbitalhq.formats.Xml
               model ExampleModel {
                   container: {
                       rows: {
                           row: {
                               attribute1: String,
                               attribute2: String
                           }[]
                       }
                   }
               }
            """.trimIndent()
         )
         val srcWithTwoRows = """<root>
   <container>
      <rows>
         <row attribute1="value1" attribute2="value2" />
         <row attribute1="value3" attribute2="value4" />
      </rows>
   </container>
</root>"""
         val typedInstance = TypedInstance.from(
            schema.type("ExampleModel"), srcWithTwoRows, schema,
            source = Provided,
            formatSpecs = listOf(XmlFormatSpec)
         )
         typedInstance.toRawObject()
            .shouldBe(
               mapOf(
                  "container" to mapOf(
                     "rows" to mapOf(
                        "row" to listOf(
                           mapOf("attribute1" to "value1", "attribute2" to "value2"),
                           mapOf("attribute1" to "value3", "attribute2" to "value4")
                        )
                     )
                  )
               )
            )
         val srcWithOneRow = """<root>
   <container>
      <rows>
         <row attribute1="value1" attribute2="value2" />
      </rows>
   </container>
</root>"""
         val typedInstanceWithSingleRow = TypedInstance.from(
            schema.type("ExampleModel"), srcWithOneRow, schema,
            source = Provided,
            formatSpecs = listOf(XmlFormatSpec)
         )
         typedInstanceWithSingleRow
            .toRawObject()
            .shouldBe(
               mapOf(
                  "container" to mapOf(
                     "rows" to mapOf(
                        "row" to listOf(
                           mapOf("attribute1" to "value1", "attribute2" to "value2"),
                        )
                     )
                  )
               )
            )
      }
   }
})

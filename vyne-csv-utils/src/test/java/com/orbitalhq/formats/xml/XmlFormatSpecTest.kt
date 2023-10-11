package com.orbitalhq.formats.xml

import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.winterbe.expekt.should
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class XmlFormatSpecTest : DescribeSpec({
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
                @XmlAttribute
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
  <actors>
    <actor id="1">
      <firstName>Mel</firstName>
      <lastName>Gibson</lastName>
      <agent>
        <firstName>Johnny</firstName>
        <lastName>Cashpott</lastName>
      </agent>
    </actor>
    <actor id="2">
      <firstName>Jack</firstName>
      <lastName>Spratt</lastName>
      <agent>
        <firstName>Johnny</firstName>
        <lastName>Cashpott</lastName>
      </agent>
    </actor>
  </actors>
  <title>Star Wars</title>
</movie>
         """.trimIndent()

         val parsed = TypedInstance.from(schema.type("Movie"), xml, schema, formatSpecs = listOf(XmlFormatSpec))
         parsed.toRawObject().shouldBe(
            mapOf(
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

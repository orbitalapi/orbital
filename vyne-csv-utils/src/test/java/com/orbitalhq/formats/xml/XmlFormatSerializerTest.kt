package com.orbitalhq.formats.xml

import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.from
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.test.utils.shouldEqualIgnoringWhitespace
import com.winterbe.expekt.should
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
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
         val xml = XmlFormatSpec.serializer.write(typedInstance, mock {  }, schema, -1) as String
         val expected = """<?xml version='1.0' encoding='UTF-8'?><Person><firstName>Jimmy</firstName><lastName>Smith</lastName></Person>"""
         xml.shouldBe(expected)
      }
      it("should write simple object with attribute to xml") {
         val typedInstance = TypedInstance.from(schema.type("Actor"),
            mapOf("firstName" to "Jimmy", "lastName" to "Smith", "id" to 3),
            schema
         )
         val xml = XmlFormatSpec.serializer.write(typedInstance, mock {  }, schema, -1) as String
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
         val xml = XmlFormatSpec.serializer.write(typedInstance, mock {  }, schema, -1) as String
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
   describe("serializing to xml with namespaces") {
      val schemaWithNamespaces = TaxiSchema.from(
         """

         // Type does not declare an Xml namespace
         type DurationInMinutes inherits Int

         @lang.taxi.xml.XmlNamespace(uri = "http://example.com/movies")
         type MovieTitle inherits String

         @lang.taxi.xml.XmlNamespace(uri = "http://example.com/movies")
         @com.orbitalhq.formats.Xml
         model Movie {
             title : MovieTitle
             duration : DurationInMinutes
             director : Director
         }

         @lang.taxi.xml.XmlNamespace(uri = "http://example.com/people")
         model Director {
             name : DirectorName
             age : Age
         }

         @lang.taxi.xml.XmlNamespace(uri = "http://example.com/people")
         type DirectorName inherits String
         @lang.taxi.xml.XmlNamespace(uri = "http://example.com/people")
         type Age inherits Int

         @lang.taxi.xml.XmlNamespace(uri = "http://example.com/movies")
         model SimpleMovie {
             title : MovieTitle
             duration: DurationInMinutes
         }
      """.trimIndent()
      )

      it("should write object with namespace to xml") {
         val typedInstance = TypedInstance.from(
            schemaWithNamespaces.type("SimpleMovie"),
            mapOf("title" to "Inception", "duration" to 180),

            schemaWithNamespaces
         )
         val xml = XmlFormatSpec.serializer.write(typedInstance, mock {}, schemaWithNamespaces, -1) as String

         xml.shouldEqualIgnoringWhitespace("""<?xml version='1.0' encoding='UTF-8'?>
<ns0:SimpleMovie xmlns:ns0="http://example.com/movies">
    <ns0:title>Inception</ns0:title>
    <duration>180</duration>
</ns0:SimpleMovie>""")
      }

      it("should write object with mixed namespaces to xml") {
         val typedInstance = TypedInstance.from(
            schemaWithNamespaces.type("Movie"),
            mapOf(
               "title" to "The Matrix",
               "director" to mapOf(
                  "name" to "Wachowski",
                  "age" to 55
               )
            ),
            schemaWithNamespaces
         )
         val xml = XmlFormatSpec.serializer.write(typedInstance, mock {}, schemaWithNamespaces, -1) as String

         // Should have both namespace declarations
         xml.shouldEqualIgnoringWhitespace("""<?xml version='1.0' encoding='UTF-8'?>
<ns0:Movie xmlns:ns0="http://example.com/movies" xmlns:ns1="http://example.com/people">
    <ns0:title>The Matrix</ns0:title>
    <ns1:director>
        <ns1:name>Wachowski</ns1:name>
        <ns1:age>55</ns1:age>
    </ns1:director>
</ns0:Movie>
""")
      }

      it("should handle objects without namespaces alongside namespaced objects") {
         val mixedSchema = TaxiSchema.from(
            """
            @lang.taxi.xml.XmlNamespace(uri = "http://example.com/movies")
            @com.orbitalhq.formats.Xml
            model Movie {
                title : MovieTitle inherits String
                metadata : Metadata
            }

            model Metadata {
                rating : Rating inherits String
                year : Year inherits Int
            }
         """.trimIndent()
         )

         val typedInstance = TypedInstance.from(
            mixedSchema.type("Movie"),
            mapOf(
               "title" to "Blade Runner",
               "metadata" to mapOf(
                  "rating" to "R",
                  "year" to 1982
               )
            ),
            mixedSchema
         )
         val xml = XmlFormatSpec.serializer.write(typedInstance, mock {}, mixedSchema, -1) as String

         // Movie should have namespace, but metadata and title should not
         xml.shouldEqualIgnoringWhitespace("""<?xml version='1.0' encoding='UTF-8'?>
<ns0:Movie xmlns:ns0="http://example.com/movies">
    <title>Blade Runner</title>
    <metadata>
        <rating>R</rating>
        <year>1982</year>
    </metadata>
</ns0:Movie>""")

      }

      it("should write attributes with namespaced parent") {
         val schemaWithAttributes = TaxiSchema.from(
            """
            @lang.taxi.xml.XmlNamespace(uri = "http://example.com/actors")
            @com.orbitalhq.formats.Xml
            model Actor {
                @lang.taxi.xml.XmlAttribute
                id : ActorId inherits Int
                name : ActorName inherits String
            }
         """.trimIndent()
         )

         val typedInstance = TypedInstance.from(
            schemaWithAttributes.type("Actor"),
            mapOf("id" to 42, "name" to "Harrison Ford"),
            schemaWithAttributes
         )
         val xml = XmlFormatSpec.serializer.write(typedInstance, mock {}, schemaWithAttributes, -1) as String
xml.shouldEqualIgnoringWhitespace("""
   <?xml version='1.0' encoding='UTF-8'?><ns0:Actor xmlns:ns0="http://example.com/actors" id="42"><name>Harrison Ford</name></ns0:Actor>
""".trimIndent())
      }

      it("should write collections with namespaces") {
         val schemaWithCollections = TaxiSchema.from(
            """
            @lang.taxi.xml.XmlNamespace(uri = "http://example.com/movies")
            @com.orbitalhq.formats.Xml
            model MovieList {
                movies : Movie[]
            }

            @lang.taxi.xml.XmlNamespace(uri = "http://example.com/movies")
            type MovieTitle inherits String

            @lang.taxi.xml.XmlNamespace(uri = "http://example.com/movies")
            model Movie {
                title : MovieTitle
            }
         """.trimIndent()
         )

         val typedInstance = TypedInstance.from(
            schemaWithCollections.type("MovieList"),
            mapOf(
               "movies" to listOf(
                  mapOf("title" to "Star Wars"),
                  mapOf("title" to "Jaws")
               )
            ),
            schemaWithCollections
         )
         val xml = XmlFormatSpec.serializer.write(typedInstance, mock {}, schemaWithCollections, -1) as String
         xml.shouldEqualIgnoringWhitespace("""<?xml version='1.0' encoding='UTF-8'?>
<ns0:MovieList xmlns:ns0="http://example.com/movies">
    <ns0:movies>
        <ns0:title>Star Wars</ns0:title>
    </ns0:movies>
    <ns0:movies>
        <ns0:title>Jaws</ns0:title>
    </ns0:movies>
</ns0:MovieList>""")
      }

      it("should handle deeply nested objects with different namespaces") {
         val deepSchema = TaxiSchema.from(
            """
            @lang.taxi.xml.XmlNamespace(uri = "http://example.com/level1")
            @com.orbitalhq.formats.Xml
            model Level1 {
                value : String
                level2 : Level2
            }

            @lang.taxi.xml.XmlNamespace(uri = "http://example.com/level2")
            model Level2 {
                value : String
                level3 : Level3
            }

            @lang.taxi.xml.XmlNamespace(uri = "http://example.com/level3")
            model Level3 {
                value : String
            }
         """.trimIndent()
         )

         val typedInstance = TypedInstance.from(
            deepSchema.type("Level1"),
            mapOf(
               "value" to "L1",
               "level2" to mapOf(
                  "value" to "L2",
                  "level3" to mapOf(
                     "value" to "L3"
                  )
               )
            ),
            deepSchema
         )
         val xml = XmlFormatSpec.serializer.write(typedInstance, mock {}, deepSchema, -1) as String

         xml.shouldEqualIgnoringWhitespace("""<?xml version='1.0' encoding='UTF-8'?>
<ns0:Level1 xmlns:ns0="http://example.com/level1" xmlns:ns1="http://example.com/level2" xmlns:ns2="http://example.com/level3">
    <value>L1</value>
    <ns1:level2>
        <value>L2</value>
        <ns2:level3>
            <value>L3</value>
        </ns2:level3>
    </ns1:level2>
</ns0:Level1>
""")

      }

      it("should reuse namespace prefixes for same namespace URI") {
         val sameNsSchema = TaxiSchema.from(
            """
            @lang.taxi.xml.XmlNamespace(uri = "http://example.com/common")
            @com.orbitalhq.formats.Xml
            model Container {
                item1 : Item1
                item2 : Item2
            }

            @lang.taxi.xml.XmlNamespace(uri = "http://example.com/common")
            model Item1 {
                value : String
            }

            @lang.taxi.xml.XmlNamespace(uri = "http://example.com/common")
            model Item2 {
                value : String
            }
         """.trimIndent()
         )

         val typedInstance = TypedInstance.from(
            sameNsSchema.type("Container"),
            mapOf(
               "item1" to mapOf("value" to "Value1"),
               "item2" to mapOf("value" to "Value2")
            ),
            sameNsSchema
         )
         val xml = XmlFormatSpec.serializer.write(typedInstance, mock {}, sameNsSchema, -1) as String

         // Should only declare ns0 once, and no ns1
         xml.shouldEqualIgnoringWhitespace("""<?xml version='1.0' encoding='UTF-8'?>
<ns0:Container xmlns:ns0="http://example.com/common">
    <ns0:item1>
        <value>Value1</value>
    </ns0:item1>
    <ns0:item2>
        <value>Value2</value>
    </ns0:item2>
</ns0:Container>
""")
      }

      it("should set elements in the namespace of the object when underlying type comes from a different namespace") {
         val complexSchema = TaxiSchema.from("""
namespace com.example {
   @lang.taxi.xml.Xml
   @lang.taxi.xml.XmlNamespace(uri = "http://example.com/main")
   closed model ItemType {
      @lang.taxi.xml.XmlNamespace(uri = "http://example.com/main") IsActive : com.example.itemtype.IsActive
      [[ Purchase price of property, as stated on application form. ]]
      @lang.taxi.xml.XmlNamespace(uri = "http://example.com/main") PurchasePrice : com.example.itemtype.PurchasePrice?
   }

   [[ Y or N ]]
   @lang.taxi.xml.XmlNamespace(uri = "http://example.com/common")
   enum YNType {
      `Y`,
      `N`
   }

   type HSFStdSterlingAmountType inherits Int

   @lang.taxi.xml.Xml
   @lang.taxi.xml.XmlNamespace(uri = "http://example.com/main")
   closed model Document {
      [[ Common header element ]]
      @lang.taxi.xml.XmlNamespace(uri = "http://example.com/common") Header : Header
      @lang.taxi.xml.XmlNamespace(uri = "http://example.com/main") Item : com.example.document.Item
   }

   @lang.taxi.xml.Xml
   @lang.taxi.xml.XmlNamespace(uri = "http://example.com/common")
   closed model Header {
      @lang.taxi.xml.XmlNamespace(uri = "http://example.com/common") Version : com.example.header.Version
      @lang.taxi.xml.XmlNamespace(uri = "http://example.com/common") Timestamp : com.example.header.Timestamp
   }


}
namespace com.example.itemtype {
   enum IsActive inherits com.example.YNType
   type PurchasePrice inherits com.example.HSFStdSterlingAmountType
}
namespace com.example.header {
   type Version inherits String
   type Timestamp inherits DateTime
}
namespace com.example.document {
   type Item inherits com.example.ItemType
}
         """.trimIndent())
         val testData = mapOf(
            "Header" to mapOf(
               "Version" to "1.0",
               "Timestamp" to "2025-10-23T10:30:00"
            ),
            "Item" to mapOf(
               "IsActive" to "Y",
               "PurchasePrice" to 5000
            )
         )
         val instance = TypedInstance.from(complexSchema.type("com.example.Document"), testData, complexSchema)
         val xml = XmlFormatSpec.serializer.write(instance, mock {}, complexSchema, -1) as String
         xml.shouldEqualIgnoringWhitespace("""<?xml version='1.0' encoding='UTF-8'?>
<ns0:Document xmlns:ns0="http://example.com/main" xmlns:ns1="http://example.com/common">
    <ns1:Header>
        <ns1:Version>1.0</ns1:Version>
        <ns1:Timestamp>2025-10-23T10:30</ns1:Timestamp>
    </ns1:Header>
    <ns0:Item>
        <ns0:IsActive>Y</ns0:IsActive>
        <ns0:PurchasePrice>5000</ns0:PurchasePrice>
    </ns0:Item>
</ns0:Document>
""")
      }
   }

})



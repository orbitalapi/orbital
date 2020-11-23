package io.vyne.vyneql

import arrow.core.Either
import com.winterbe.expekt.should
import lang.taxi.Compiler
import lang.taxi.types.QualifiedName
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.fail

object VyneQlGrammarSpek : Spek({
   describe("basic query grammar") {
      val schema = """
         namespace foo

         type InsertedAt inherits Instant
         type TraderId inherits String
         type UserEmail inherits String
         type FirstName inherits String
         type LastName inherits String

         type Customer {
            email : CustomerEmailAddress as String
         }
         type Trade

         type OutputOrder {
         }
         type Order {
            tradeTimestamp : TradeDate as Instant
            traderId: TraderId
         }

         type Trade {
            traderId: TraderId
         }
      """.trimIndent()
      val taxi = Compiler(schema).compile()
      it("should compile a simple query") {
         val src = """
            findAll { Order }
         """.trimIndent()
         val query = VyneQlCompiler(src, taxi).query()
         query.queryMode.should.equal(QueryMode.FIND_ALL)
      }

      it("should resolve unambiguous types without imports") {
         val src = """
            query RecentOrdersQuery( startDate:Instant, endDate:Instant ) {
               findAll {
                  Order[]( TradeDate >= startDate , TradeDate < endDate )
               } as OutputOrder[]
            }
         """.trimIndent()
         val query = VyneQlCompiler(src, taxi).query()
         query.parameters.should.have.size(2)
         query.parameters.should.equal(mapOf("startDate" to QualifiedName.from("lang.taxi.Instant"), "endDate" to QualifiedName.from("lang.taxi.Instant")))
         query.projectedType?.concreteTypeName?.parameterizedName.should.equal("lang.taxi.Array<foo.OutputOrder>")
         query.typesToFind.should.have.size(1)
         query.typesToFind.first().type.parameterizedName.should.equal("lang.taxi.Array<foo.Order>")
      }


      it("should compile a named query with params") {
         val src = """
            import foo.Order
            import foo.OutputOrder
            import foo.TradeDate

            query RecentOrdersQuery( startDate:Instant, endDate:Instant ) {
               findAll {
                  Order[]( TradeDate >= startDate , TradeDate < endDate )
               } as OutputOrder[]
            }
         """.trimIndent()
         val query = VyneQlCompiler(src, taxi).query()
         query.name.should.equal("RecentOrdersQuery")
         query.parameters.should.have.size(2)
         query.parameters.should.equal(mapOf("startDate" to QualifiedName.from("lang.taxi.Instant"), "endDate" to QualifiedName.from("lang.taxi.Instant")))
         query.projectedType?.concreteTypeName?.parameterizedName.should.equal("lang.taxi.Array<foo.OutputOrder>")
         query.typesToFind.should.have.size(1)

         val typeToFind = query.typesToFind.first()
         typeToFind.type.parameterizedName.should.equal("lang.taxi.Array<foo.Order>")
         typeToFind.constraints.should.have.size(2)
      }

      it("should compile a query that exposes facts") {
         val src = """
            given {
               email : CustomerEmailAddress = "jimmy@demo.com"
            }
            findAll { Trade }
         """.trimIndent()
         val query = VyneQlCompiler(src, taxi).query()
         query.facts.should.have.size(1)
         val (name, fact) = query.facts.entries.first()
         name.should.equal("email")
         fact.type.fullyQualifiedName.should.equal("foo.CustomerEmailAddress")
         fact.value.should.equal("jimmy@demo.com")
      }

      it("should compile an unnamed query") {
         val src = """
            import foo.Order
            import foo.OutputOrder

            findAll {
               Order[]( TradeDate  >= startDate , TradeDate < endDate )
            }
      """.trimIndent()
         VyneQlCompiler(src, taxi).query()
      }

      it("Should Allow anonymous projected type definition") {
         val src = """
            import foo.Order
            import foo.OutputOrder

            findAll {
               Order[]( TradeDate  >= startDate , TradeDate < endDate )
            } as {
               tradeTimestamp
            }[]
      """.trimIndent()
         val query = VyneQlCompiler(src, taxi).query()
         query.projectedType!!.concreteTypeName.should.be.`null`
         query.projectedType!!.anonymousTypeDefinition!!.isList.should.be.`true`
         query.projectedType!!.anonymousTypeDefinition!!.fields.first().should.equal(
            SimpleAnonymousFieldDefinition("tradeTimestamp", QualifiedName.from("foo.TradeDate"))
         )
      }

      it("Should not Allow anonymous projected type definitions with invalid field references") {
         val src = """
            import foo.Order
            import foo.OutputOrder

            findAll {
               Order[]( TradeDate  >= startDate , TradeDate < endDate )
            } as {
               invalidField
            }[]
      """.trimIndent()
         val query = when (val result = VyneQlCompiler(src, taxi).compile()) {
            is Either.Left -> result.a
            else -> fail()
         }

         query.first().detailMessage.should.equal("invalidField is not part of foo.Order")
      }

      it("Should Allow anonymous type that extends base type") {
         val src = """
            import foo.Order
            import foo.OutputOrder

            findAll {
               Order[]( TradeDate  >= startDate , TradeDate < endDate )
            } as OutputOrder {
               tradeTimestamp
            }[]
      """.trimIndent()
         val query = VyneQlCompiler(src, taxi).query()
         query.projectedType!!.concreteTypeName!!.fullyQualifiedName.should.equal("foo.OutputOrder")
         query.projectedType!!.anonymousTypeDefinition!!.isList.should.be.`true`
         query.projectedType!!.anonymousTypeDefinition!!.fields.first().should.equal(
            SimpleAnonymousFieldDefinition("tradeTimestamp", QualifiedName.from("foo.TradeDate"))
         )
      }

      it("Should Not Allow anonymous type that extends base type when anonymous type reference a field that does not part of discovery type") {
         val src = """
            import foo.Order
            import foo.OutputOrder

            findAll {
               Order[]( TradeDate  >= startDate , TradeDate < endDate )
            } as OutputOrder {
               invalidField
            }[]
      """.trimIndent()
         val query = when (val result = VyneQlCompiler(src, taxi).compile()) {
            is Either.Left -> result.a
            else -> fail()
         }

         query.first().detailMessage.should.equal("invalidField is not part of foo.Order")
      }

      it("Should Allow anonymous type that extends a base type and adds additional field definitions") {
         val src = """
            import foo.Order
            import foo.OutputOrder

            findAll {
               Order[]( TradeDate  >= startDate , TradeDate < endDate )
            } as OutputOrder {
               insertedAt: foo.InsertedAt
            }[]
      """.trimIndent()
         val query = VyneQlCompiler(src, taxi).query()
         query.projectedType!!.concreteTypeName!!.fullyQualifiedName.should.equal("foo.OutputOrder")
         query.projectedType!!.anonymousTypeDefinition!!.isList.should.be.`true`
         query.projectedType!!.anonymousTypeDefinition!!.fields.first().should.equal(
            SimpleAnonymousFieldDefinition("insertedAt", QualifiedName.from("foo.InsertedAt"))
         )
      }

      it("Should Allow anonymous type with field definitions") {
         val src = """
            import foo.Order
            import foo.OutputOrder

            findAll {
               Order[]( TradeDate  >= startDate , TradeDate < endDate )
            } as {
               insertedAt: foo.InsertedAt
            }[]
      """.trimIndent()
         val query = VyneQlCompiler(src, taxi).query()
         query.projectedType!!.concreteTypeName.should.be.`null`
         query.projectedType!!.anonymousTypeDefinition!!.isList.should.be.`true`
         query.projectedType!!.anonymousTypeDefinition!!.fields.first().should.equal(
            SimpleAnonymousFieldDefinition("insertedAt", QualifiedName.from("foo.InsertedAt"))
         )
      }

      it("Should Allow anonymous type with field definitions referencing type to discover") {
         val src = """
            import foo.Order
            import foo.OutputOrder

            findAll {
               Order[]( TradeDate  >= startDate , TradeDate < endDate )
            } as {
               traderEmail: UserEmail (from this.traderId)
            }[]
      """.trimIndent()
         val query = VyneQlCompiler(src, taxi).query()
         query.projectedType!!.concreteTypeName.should.be.`null`
         query.projectedType!!.anonymousTypeDefinition!!.isList.should.be.`true`
         query.projectedType!!.anonymousTypeDefinition!!.fields.first().should.equal(
            SelfReferencedFieldDefinition(
               fieldName = "traderEmail",
               fieldType = QualifiedName.from("foo.UserEmail"),
               referenceFieldName = "traderId",
               referenceFieldContainingType = QualifiedName.from("foo.Order"))
         )
      }

      it("Should Allow anonymous type with field definitions referencing projected type") {
         val src = """
            import foo.Order
            import foo.OutputOrder

            findAll {
               Order[]( TradeDate  >= startDate , TradeDate < endDate )
            } as foo.Trade {
               traderEmail: UserEmail (from this.traderId)
            }[]
      """.trimIndent()
         val query = VyneQlCompiler(src, taxi).query()
         query.projectedType!!.concreteTypeName.should.equal(QualifiedName.from("foo.Trade"))
         query.projectedType!!.anonymousTypeDefinition!!.isList.should.be.`true`
         query.projectedType!!.anonymousTypeDefinition!!.fields.first().should.equal(
            SelfReferencedFieldDefinition(
               fieldName = "traderEmail",
               fieldType = QualifiedName.from("foo.UserEmail"),
               referenceFieldName = "traderId",
               referenceFieldContainingType = QualifiedName.from("foo.Trade"))
         )
      }

      it("Should Fail anonymous type with field definitions referencing projected type but have invalid field type") {
         val src = """
            import foo.Order
            import foo.OutputOrder

            findAll {
               Order[]( TradeDate  >= startDate , TradeDate < endDate )
            } as foo.Trade {
               traderEmail: InvalidType (from this.traderId)
            }[]
      """.trimIndent()

         val query = when (val result = VyneQlCompiler(src, taxi).compile()) {
            is Either.Left -> result.a
            else -> fail()
         }

         query.first().detailMessage.should.equal("Type InvalidType could not be resolved")
      }

      it("Should Allow anonymous type with complex field definitions referencing type to be discovered") {
         val src = """
            import foo.Order
            import foo.OutputOrder

            findAll {
               Order[]( TradeDate  >= startDate , TradeDate < endDate )
            } as {
                   salesPerson {
                       firstName : FirstName
                       lastName : LastName
                   }(from this.traderId)
            }[]
      """.trimIndent()
         val query = VyneQlCompiler(src, taxi).query()
         query.projectedType!!.concreteTypeName.should.be.`null`
         query.projectedType!!.anonymousTypeDefinition!!.isList.should.be.`true`
         query.projectedType!!.anonymousTypeDefinition!!.fields.first().should.equal(
            ComplexFieldDefinition(
               fieldName = "salesPerson",
               referenceFieldName = "traderId",
               referenceFieldContainingType = QualifiedName.from("foo.Order"),
               fieldDefinitions = listOf(
                  SimpleAnonymousFieldDefinition(fieldName = "firstName", fieldType = QualifiedName.from("foo.FirstName")),
                  SimpleAnonymousFieldDefinition(fieldName = "lastName", fieldType = QualifiedName.from("foo.LastName")))
            )
         )
      }

      it("should handle queries of array types with long syntax") {
         val src = """
            import foo.Order

            findAll { lang.taxi.Array<Order> }
         """.trimIndent()
         val query = VyneQlCompiler(src,taxi).query()
         query.typesToFind[0].type.parameterizedName.should.equal("lang.taxi.Array<foo.Order>")
      }
      it("should handle queries of array types with short syntax") {
         val src = """
            import foo.Order

            findAll { Order[] }
         """.trimIndent()
         val query = VyneQlCompiler(src,taxi).query()
         query.typesToFind[0].type.parameterizedName.should.equal("lang.taxi.Array<foo.Order>")
      }

      it("Should Allow anonymous type with complex field definitions referencing projected type") {
         val src = """
            import foo.Order
            import foo.OutputOrder

            findAll {
               Order[]( TradeDate  >= startDate , TradeDate < endDate )
            } as foo.Trade {
                   salesPerson {
                       firstName : FirstName
                       lastName : LastName
                   }(from this.traderId)
            }[]
      """.trimIndent()
         val query = VyneQlCompiler(src, taxi).query()
         query.projectedType!!.concreteTypeName.should.equal(QualifiedName.from("foo.Trade"))
         query.projectedType!!.anonymousTypeDefinition!!.isList.should.be.`true`
         query.projectedType!!.anonymousTypeDefinition!!.fields.first().should.equal(
            ComplexFieldDefinition(
               fieldName = "salesPerson",
               referenceFieldName = "traderId",
               referenceFieldContainingType = QualifiedName.from("foo.Trade"),
               fieldDefinitions = listOf(
                  SimpleAnonymousFieldDefinition(fieldName = "firstName", fieldType = QualifiedName.from("foo.FirstName")),
                  SimpleAnonymousFieldDefinition(fieldName = "lastName", fieldType = QualifiedName.from("foo.LastName")))
            )
         )
      }

      it("Should Detect anonymous type with invalid complex field definitions referencing projected type") {
         val src = """
            import foo.Order
            import foo.OutputOrder

            findAll {
               Order[]( TradeDate  >= startDate , TradeDate < endDate )
            } as foo.Trade {
                   salesPerson {
                       firstName : InvalidType
                       lastName : LastName
                   }(from this.traderId)
            }[]
      """.trimIndent()
         val query = when (val result = VyneQlCompiler(src, taxi).compile()) {
            is Either.Left -> result.a
            else -> fail()
         }

         query.first().detailMessage.should.equal("Type InvalidType could not be resolved")
      }

      it("discovery type and projected type should either be list or be single entity II") {
         val src = """
            query RecentOrdersQuery( startDate:Instant, endDate:Instant ) {
               findAll {
                  Order[]( TradeDate >= startDate , TradeDate < endDate )
               } as OutputOrder
            }
         """.trimIndent()
         val query = when (val result = VyneQlCompiler(src, taxi).compile()) {
            is Either.Left -> result.a
            else -> fail()
         }

         query.first().detailMessage.should.equal("projection type is a list but the type to discover is not, both should either be list or single entity.")
      }

      it("discovery type and anonymous projected type should either be list or be single entity II") {
         val src = """
            query RecentOrdersQuery( startDate:Instant, endDate:Instant ) {
               findAll {
                  Order[]( TradeDate >= startDate , TradeDate < endDate )
               } as {
                  insertedAt: foo.InsertedAt
               }
            }
         """.trimIndent()
         val query = when (val result = VyneQlCompiler(src, taxi).compile()) {
            is Either.Left -> result.a
            else -> fail()
         }

         query.first().detailMessage.should.equal("projection type is a list but the type to discover is not, both should either be list or single entity.")
      }
   }
})

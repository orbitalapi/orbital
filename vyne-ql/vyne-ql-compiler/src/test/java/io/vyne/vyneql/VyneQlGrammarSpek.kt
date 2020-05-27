package io.vyne.vyneql

import com.winterbe.expekt.should
import lang.taxi.Compiler
import lang.taxi.types.QualifiedName
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object VyneQlGrammarSpek : Spek({
   describe("basic query grammar") {
      val schema = """
         namespace foo

         type OutputOrder {
         }
         type Order {
            tradeTimestamp : TradeDate as Instant
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

      it("should compile an anonymous query") {
         val src = """
            findAll {
                  Order[]( TradeDate >= startDate , TradeDate < endDate )
               } as OutputOrder[]
         """.trimIndent()
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
         query.projectedType!!.parameterizedName.should.equal("lang.taxi.Array<foo.OutputOrder>")
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
         query.projectedType!!.parameterizedName.should.equal("lang.taxi.Array<foo.OutputOrder>")
         query.typesToFind.should.have.size(1)

         val typeToFind = query.typesToFind.first()
         typeToFind.type.parameterizedName.should.equal("lang.taxi.Array<foo.Order>")
         typeToFind.constraints.should.have.size(2)
      }

      it("should compile an unnamed query") {
         val src = """
            import foo.Order
            import foo.OutputOrder

            findAll {
               Order[]( TradeDate  >= startDate , TradeDate < $\endDate )
            }
      """.trimIndent()
         val query = VyneQlCompiler(src, taxi).query()
      }
   }


})

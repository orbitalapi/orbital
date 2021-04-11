package io.vyne.schemas

import com.winterbe.expekt.should
import io.vyne.VersionedSource
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class DefaultTypeCacheTest {
   val model1Source = """
         namespace foo {
            model Model {
              age: Int
            }
         }
      """.trimIndent()

   val model2Source = """
         namespace bar {
            type FirstName inherits String
            model Model {
              name:  FirstName
            }

            type extension Model {
              name:  FirstName by default ('jimmy')
            }
         }
      """.trimIndent()
   val schema = TaxiSchema.from(
      listOf(
         VersionedSource("model1.taxi", "1.0.0", model1Source),
         VersionedSource("model2.taxi", "1.0.0", model2Source)
      )
   )

   @Test
   fun `DefaultCache cannot resolve from shortNames Cache when there are two types with the same short name`() {
      val cache = DefaultTypeCache(schema.types)
      cache.fromShortName("Model".fqn()).should.be.`null`
   }

   @Test
   fun `DefaultCache can resolve from shortNames Cache when there are only one type with the given short name`() {
      val cache = DefaultTypeCache(schema.types)
      cache.fromShortName("FirstName".fqn()).should.not.be.`null`
   }

   @Test
   fun `should resolve fqn to type`() {
      val cache = DefaultTypeCache(schema.types)
      val barModel = cache.type("bar.Model")
      barModel.should.not.be.`null`
      barModel.attributes["name"].should.not.be.`null`
      val fooModel = cache.type("foo.Model")
      fooModel.should.not.be.`null`
      fooModel.attributes["age"].should.not.be.not
   }

   @Test
   fun `should cache default values`() {
      val cache = DefaultTypeCache(schema.types)
      cache.defaultValues("bar.Model".fqn())!!.values.first().value.should.equal("jimmy")
   }
}
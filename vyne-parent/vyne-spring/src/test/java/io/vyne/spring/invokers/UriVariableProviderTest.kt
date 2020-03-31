package io.vyne.spring.invokers

import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.expect
import io.vyne.models.TypedValue
import io.vyne.schemas.Parameter
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import org.junit.Test

class UriVariableProviderTest {

   val provider = UriVariableProvider()

   @Test
   fun matchesBasedOnType() {
      val int = type("lang.taxi.Int")
      val params = listOf(Parameter(int) to TypedValue.from(int, 5))
      val url = "http://foo.com/bar/{lang.taxi.Int}"

      val variables = provider.getUriVariables(params, url)
      expect(variables["lang.taxi.Int"]!!).to.equal(5)
   }

   @Test
   fun matchesBasedOnName() {
      val int = type("lang.taxi.Int")
      val params = listOf(Parameter(int, name = "id") to TypedValue.from(int, 5))
      val url = "http://foo.com/bar/{id}"

      val variables = provider.getUriVariables(params, url)
      expect(variables["id"]!!).to.equal(5)
   }

   @Test
   fun findsAllMatches() {
      val url = "http://foo.com/bar/{id}/something/{lang.taxi.Int}"
      val matches = provider.findVariableNames(url)
      expect(matches).to.have.size(2)
      expect(matches).to.equal(listOf("id","lang.taxi.Int"))
   }


   fun type(name: String): Type {
      return Type(name.fqn(), sources = emptyList(), typeDoc = null, taxiType = mock())
   }
}

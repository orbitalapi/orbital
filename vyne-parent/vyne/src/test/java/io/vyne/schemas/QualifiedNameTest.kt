package io.vyne.schemas

import com.winterbe.expekt.expect
import org.junit.Test

class QualifiedNameTest {

   @Test
   fun parsesNamesCorrectly() {
      val fqn = "foo.baz.Bar".fqn()
      expect(fqn.name).to.equal("Bar")
      expect(fqn.fullyQualifiedName).to.equal("foo.baz.Bar")
      expect(fqn.parameters).to.be.empty
   }

   @Test
   fun parsesParamterizedNamesCorrectly() {
      val fqn = "foo.baz.Bar<some.Thing<a.B,c.D>,some.other.Thing<d.E,f.G>>".fqn()
      expect(fqn.parameters).to.have.size(2)

      expect(fqn.parameters[0].fullyQualifiedName).to.equal("some.Thing")
      expect(fqn.parameters[0].parameters).to.have.size(2)
      expect(fqn.parameters[0].parameters[0].fullyQualifiedName).to.equal("a.B")
      expect(fqn.parameters[0].parameters[1].fullyQualifiedName).to.equal("c.D")

      expect(fqn.parameters[1].fullyQualifiedName).to.equal("some.other.Thing")
      expect(fqn.parameters[1].parameters).to.have.size(2)
      expect(fqn.parameters[1].parameters[0].fullyQualifiedName).to.equal("d.E")
      expect(fqn.parameters[1].parameters[1].fullyQualifiedName).to.equal("f.G")
   }

   @Test
   fun parsesArrayShorthandCorrectly() {
      val fqn = "sample.Foo[]".fqn()
      expect(fqn.parameters).to.have.size(1)
      expect(fqn.parameters[0].fullyQualifiedName).to.equal("sample.Foo")
      expect(fqn.name).to.equal("Array")
   }
}

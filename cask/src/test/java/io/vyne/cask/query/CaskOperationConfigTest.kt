package io.vyne.cask.query

import com.nhaarman.mockitokotlin2.whenever
import com.winterbe.expekt.should
import lang.taxi.types.Type
import io.vyne.cask.query.generators.OperationAnnotation
import io.vyne.cask.query.generators.OperationGeneratorConfig
import org.junit.Test
import org.mockito.Mockito.mock

class CaskOperationConfigTest {
   @Test
   fun canReadOperationsConfig() {
      val someType = mock(Type::class.java)
      val someOtherType = mock(Type::class.java)

      whenever(someType.qualifiedName).thenReturn("someType")
      whenever(someOtherType.qualifiedName).thenReturn("someOtherType")

      val config = OperationGeneratorConfig(
         listOf(
            OperationGeneratorConfig.OperationConfigDefinition(someType.qualifiedName, OperationAnnotation.After),
            OperationGeneratorConfig.OperationConfigDefinition(someType.qualifiedName, OperationAnnotation.Between),
            OperationGeneratorConfig.OperationConfigDefinition(someOtherType.qualifiedName, OperationAnnotation.Association)
         ))

      config.definesOperation(someType, OperationAnnotation.After).should.be.`true`
      config.definesOperation(someType, OperationAnnotation.Between).should.be.`true`
      config.definesOperation(someOtherType, OperationAnnotation.Association).should.be.`true`
      config.definesOperation(someType, OperationAnnotation.Association).should.be.`false`
      config.definesOperation(someOtherType, OperationAnnotation.Between).should.be.`false`
   }
}

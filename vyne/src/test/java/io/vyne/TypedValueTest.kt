package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.TypedInstance
import io.vyne.schemas.Modifier
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.types.PrimitiveType
import org.junit.Test
import org.mockito.Mockito.mock
import java.time.Instant

class TypedValueTest {
   @Test
   fun testStringInstantConversion() {
      val schema = TaxiSchema.from("")
      val instance = TypedInstance.from(schema.type(PrimitiveType.INSTANT), "2020-05-14T22:00:00Z", schema)

      instance.value.should.equal(Instant.parse("2020-05-14T22:00:00Z"))
   }
}

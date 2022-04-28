package io.vyne.models

import com.winterbe.expekt.should
import org.junit.Test

class ConversionServiceTest {
   @Test
   fun `when using core types without conversion service then no-op conversion service is used`() {
      // Conversion services have moved to a seperate jar.
      // we try to detect at runtime if they're available, and if not,
      // fall back to a no-op converter
      ConversionService.DEFAULT_CONVERTER.should.be.instanceof(NoOpConversionService::class.java)
   }

}

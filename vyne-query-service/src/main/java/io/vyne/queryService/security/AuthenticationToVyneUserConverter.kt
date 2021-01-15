package io.vyne.queryService.security

import io.vyne.security.VyneUser
import io.vyne.security.toVyneUser
import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class AuthenticationToVyneUserConverter : Converter<Authentication, VyneUser> {
   override fun convert(source: Authentication): VyneUser {
      return source.toVyneUser()
   }
}

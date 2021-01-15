package io.vyne.queryService.security

import io.vyne.security.VyneUser
import io.vyne.security.toVyneUser
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class UserInfoService {
   @GetMapping("/api/user")
   fun currentUserInfo(
      auth: Authentication?
   ): VyneUser {
      if (auth == null) {
         throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "No user is currently logged in")
      } else {
         return auth.toVyneUser()
      }
   }
}

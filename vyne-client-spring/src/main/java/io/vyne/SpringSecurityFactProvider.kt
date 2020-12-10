package io.vyne

import io.vyne.query.Fact
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User

interface AuthenticationFactConverter {
   fun getUserFact(authentication: Authentication): Fact
}

class SpringSecurityFactProvider(private val factConverter: AuthenticationFactConverter = SpringUserDetailsFactConverter()) : FactProvider {
   override fun provideFacts(currentFacts: List<Fact>): List<Fact> {
      val context = SecurityContextHolder.getContext()
      val auth = context.authentication
      return listOf(factConverter.getUserFact(auth))
   }
}

class SpringUserDetailsFactConverter : AuthenticationFactConverter {
   override fun getUserFact(authentication: Authentication): Fact {
      val user = authentication.principal as User
      return Fact("io.vyne.Username", user.username, FactSets.CALLER)
   }
}

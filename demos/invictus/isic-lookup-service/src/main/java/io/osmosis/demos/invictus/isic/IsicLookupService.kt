package io.osmosis.demos.invictus.isic

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

enum class Scheme {
   ISIC,
   UK_SIC_2003,
   UK_SIC_2007
}

@RestController
class IsicLookupService {

   @GetMapping("/{fromScheme}/{toScheme}")
   fun convert(@PathVariable("fromScheme") fromScheme: Scheme,
               @PathVariable("toScheme") toScheme: Scheme,
               @RequestParam("code") code: String): String {
      val numberCode = code.filter { it.isDigit() }
      return "$toScheme-$numberCode"
   }
}

package io.osmosis.demos.creditInc.isic

import lang.taxi.annotations.DataType
import lang.taxi.annotations.Operation
import lang.taxi.annotations.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

// TODO : Support this type of conversion service
// See Gitlab #7
//enum class Scheme {
//   ISIC,
//   UK_SIC_2003,
//   UK_SIC_2007
//}
//
//@RestController
//class IsicLookupService {
//
//   @GetMapping("/{fromScheme}/{toScheme}")
//   fun convert(@PathVariable("fromScheme") fromScheme: Scheme,
//               @PathVariable("toScheme") toScheme: Scheme,
//               @RequestParam("code") code: String): String {
//      val numberCode = code.filter { it.isDigit() }
//      return "$toScheme-$numberCode"
//   }
//}


@RestController
@Service
class IsicConversionService {

   @GetMapping("/SIC2003/{sic2007Code}")
   @DataType("isic.uk.SIC2003")
   @Operation
   fun toSic2003(@DataType("isic.uk.SIC2003") @PathVariable("sic2007Code") input: String): String {
      val numberCode = input.filter { it.isDigit() }
      return "sic2003-$numberCode"
   }

   @GetMapping("/SIC2008/{sic2003Code}")
   @Operation
   @DataType("isic.uk.SIC2008")
   fun toSic2007(@DataType("isic.uk.SIC2003") @PathVariable("sic2003Code") input: String): String {
      val numberCode = input.filter { it.isDigit() }
      return "sic2008-$numberCode"
   }
}

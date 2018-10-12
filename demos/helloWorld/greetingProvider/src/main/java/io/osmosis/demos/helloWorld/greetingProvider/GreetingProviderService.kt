package io.osmosis.demos.helloWorld.greetingProvider

import lang.taxi.annotations.DataType
import lang.taxi.annotations.Operation
import lang.taxi.annotations.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Service
class GreetingProviderService {

   // TODO : Support zero-arg services.
//   @GetMapping("/greetings")
//   @Operation
//   fun serveGreeting():Greeting {
//      return Greeting("Hello, world")
//   }
   @Operation
   @PostMapping("/message")
   fun serveGreetingWithMessage(@RequestBody @DataType("greetings.InputMessage") message:String):Greeting {
      return Greeting("Hello, with a message: $message")
   }
}

@DataType("vyne.greeting.Greeting")
data class Greeting(@field:DataType("vyne.greeting.Message") val message:String)

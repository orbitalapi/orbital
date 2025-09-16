package com.orbitalhq

import com.orbitalhq.errors.ErrorType
import com.orbitalhq.errors.OrbitalQueryException
import com.orbitalhq.models.functions.stdlib.errors.Errors
import com.orbitalhq.query.VyneQlGrammar
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec

class EmptyArraysSpec : DescribeSpec({
   describe("Working with empty arrays") {
      it("can call a function with an empty array") {
         val (vyne, stub) = testVyne(
            VyneQlGrammar.QUERY_TYPE_TAXI,
            ErrorType.ErrorTypeDefinition,
            """
import taxi.stdlib.size
import taxi.stdlib.length
import com.orbitalhq.errors.Error
model Person {
    id: PersonId inherits Int
    name : PersonName inherits String
}

service PersonDb {
    table person:Person[]
}

model BadRequestError inherits Error {
   message: String
}

extension function requireNotEmpty(collection: Person[]):Person[] -> when {
    collection.size() == 0 -> throw((BadRequestError) { message: "Nope" }) // as Person[]
    else -> collection
}
         """.trimIndent()
         )
         stub.addTableFindManyResponse("person", "[]")
         shouldThrow<OrbitalQueryException> {
            vyne.query(
               """
            import PersonId
            given { personIds: PersonId[] = [] }
            find {
              Person[](PersonId in personIds)
                .requireNotEmpty()
            } as {
              called : PersonName
            }[]
         """.trimIndent()
            )
               .typedInstances()
         }
      }
   }
}) {
}

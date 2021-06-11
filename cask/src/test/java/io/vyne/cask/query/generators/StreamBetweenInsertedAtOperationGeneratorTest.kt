package io.vyne.cask.query.generators

import com.winterbe.expekt.should
import io.vyne.cask.ddl.schema
import io.vyne.cask.services.DefaultCaskTypeProvider
import org.junit.Test

class StreamBetweenInsertedAtOperationGeneratorTest {

    val generator = StreamBetweenInsertedAtOperationGenerator(DefaultCaskTypeProvider())

    @Test
    fun generateStreamBetweenInsertedAtOperation() {
        val (_, taxi) = schema(
            """
type Person {
    firstName : FirstName as String
    age : Int
}""".trim()
        )
        val person = taxi.objectType("Person")
        val operation = generator.generate(person)
        println(operation)

        operation.name.should.equal("streamByCaskInsertedAtBetween")
        operation.annotations.size.should.equal(1)
        operation.annotations[0].parameters["url"].should.equal("/api/cask/Person/CaskInsertedAt/streamBetween/{start}/{end}")

    }

}
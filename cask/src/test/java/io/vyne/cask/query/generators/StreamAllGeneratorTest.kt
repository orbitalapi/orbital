package io.vyne.cask.query.generators

import com.winterbe.expekt.should
import io.vyne.cask.ddl.schema
import org.junit.Test

class StreamAllGeneratorTest {

    val generator = StreamAllGenerator()

    @Test
    fun generateStreamAll() {
        val (_, taxi) = schema(
            """
type Person {
    firstName : FirstName as String
    age : Int
}""".trim()
        )
        val person = taxi.objectType("Person")
        val operation = generator.generate(person)

        operation.name.should.equal(OperationAnnotation.StreamAll.annotation)
        operation.annotations.size.should.equal(1)
        operation.annotations[0].parameters["url"].should.equal("/api/cask/streamAll/Person")

    }

}
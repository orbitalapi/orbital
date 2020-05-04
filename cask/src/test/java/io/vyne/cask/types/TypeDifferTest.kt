package io.vyne.cask.types

import com.nhaarman.mockitokotlin2.mock
import com.winterbe.expekt.should
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class TypeDifferTest {

    val differ = TypeDiffer()

    @Test
    fun diffDetectsWhenFieldIsAdded() {
        diff("Person",
                versionA = """type Person {
    firstName:String
}""".trimIndent(),
                versionB = """type Person {
            firstName : String
            lastName : String
        }""".trimIndent(),
                shouldHave = listOf(added("lastName"))
        )
    }

    @Test
    fun diffDetectsWhenFieldAddedToBaseType() {
        diff(
                typeName = "Employee",
                versionA = """
type Person {
    firstName:String
}
type Employee inherits Person
""".trimIndent(),
                versionB = """
type Person {
    firstName : String
    lastName : String
}
type Employee inherits Person
""".trimIndent(),
                shouldHave = listOf(added("lastName"))
        )
    }

    @Test
    fun diffDetectsWhenFieldIsRemoved() {
        diff(
                typeName = "Person",
                versionA = """type Person {
    firstName:String
    lastName : String
}""".trimIndent(),
                versionB = """type Person {
            firstName : String
        }""".trimIndent(),
                shouldHave = listOf(removed("lastName"))
        )
    }

    @Test
    fun diffDetectsWhenFieldRemovedFromBaseType() {
        diff(
                typeName = "Employee",
                versionA = """
type Person {
    firstName:String
    lastName : String
}
type Employee inherits Person
""".trimIndent(),
                versionB = """
type Person {
    firstName : String
}
type Employee inherits Person
""".trimIndent(),
                shouldHave = listOf(removed("lastName"))
        )
    }

    @Test
    fun diffDetectsWhenFieldIsChanged() {
        diff(
                typeName = "Person",
                versionA = """type Person {
    firstName:String
    lastName : String by column(1)
}""".trimIndent(),
                versionB = """type Person {
            firstName : String
            lastName : String by column(2)
        }""".trimIndent(),
                shouldHave = listOf(changed("lastName"))
        )
    }


    private fun diff(typeName: String, versionA: String, versionB: String, shouldHave: List<FieldDiff>) {
        val schemaA = TaxiSchema.from(versionA)
        val schemaB = TaxiSchema.from(versionB)

        val diff = differ.compareType(schemaA.taxi.type(typeName), schemaB.taxi.type("Person"))
        diff.should.have.size(shouldHave.size)

        shouldHave.forEach { expectedField ->
            require(diff.any { it::class == expectedField::class && it.fieldName == expectedField.fieldName }) {
                "expected a diff of type ${expectedField::class.simpleName} on field ${expectedField.fieldName}"
            }

        }
    }

    private fun added(fieldName: String): FieldAdded {
        return FieldAdded(fieldName, mock())
    }

    private fun removed(fieldName: String): FieldRemoved {
        return FieldRemoved(fieldName, mock())
    }

    private fun changed(fieldName: String): FieldChanged {
        return FieldChanged(fieldName, mock(), mock())
    }

}

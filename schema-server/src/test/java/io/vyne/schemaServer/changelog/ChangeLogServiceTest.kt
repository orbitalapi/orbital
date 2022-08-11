package io.vyne.schemaServer.changelog

import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.VersionedSource
import io.vyne.asPackage
import io.vyne.schemaServer.config.LocalSchemaNotifier
import io.vyne.schemaServer.packages.SchemaServerSourceManager
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.schemas.Metadata
import io.vyne.schemas.OperationNames
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.toVyneQualifiedName
import lang.taxi.types.PrimitiveType
import org.junit.Before
import org.junit.Test

class ChangeLogServiceTest {


   lateinit var changeLogService: ChangeLogService
   lateinit var sourceManager: SchemaServerSourceManager

   @Before
   fun setup() {
      val schemaStoreClient = LocalValidatingSchemaStoreClient()
      val updateNotifier = LocalSchemaNotifier(schemaStoreClient)
      sourceManager = SchemaServerSourceManager(
         emptyList(),
         mock {},
         validatingStore = schemaStoreClient,
         schemaUpdateNotifier = updateNotifier
      )
      changeLogService = ChangeLogService(updateNotifier)
   }

   @Test
   fun `verify changelog entry when new type added`() {
      val changeLogEntry = submitSource("type FirstName inherits String", "Names")!!
      changeLogEntry.diffs.single().should.equal(
         ChangeLogDiffEntry(
            displayName = "FirstName",
            kind = DiffKind.TypeAdded,
            schemaMember = "FirstName".fqn(),
            children = emptyList()
         )
      )
   }

   @Test
   fun `verify changelog entry when type removed`() {
      submitSource(
         """type FirstName inherits String
         |type LastName inherits String
      """.trimMargin(), "Names"
      )
      val changeLogEntry = submitSource(
         """type FirstName inherits String
      """.trimMargin(), "Names"
      )!!
      changeLogEntry.diffs.single().should.equal(
         ChangeLogDiffEntry(
            displayName = "LastName",
            kind = DiffKind.TypeRemoved,
            schemaMember = "LastName".fqn(),
         )
      )
   }

   @Test
   fun `verify changelog entry when new model added`() {
      submitSource(
         """type FirstName inherits String
         |type LastName inherits String
      """.trimMargin(), "Names"
      )
      val changeLogEntry = submitSource(
         """model Person {
         |firstName : FirstName
         |lastName : LastName
         |}
      """.trimMargin(), packageName = "People"
      )!!
      changeLogEntry.diffs.single().should.equal(
         ChangeLogDiffEntry(
            displayName = "Person",
            kind = DiffKind.ModelAdded,
            schemaMember = "Person".fqn(),
            children = listOf(
               ChangeLogDiffEntry("firstName", DiffKind.FieldAddedToModel, "Person".fqn()),
               ChangeLogDiffEntry("lastName", DiffKind.FieldAddedToModel, "Person".fqn()),
            )
         )
      )
   }

   @Test
   fun `verify changelog entry when field added to model`() {
      submitSource(
         """type FirstName inherits String
         |type LastName inherits String
         |model Person {
         |  firstName : FirstName
         |}
      """.trimMargin(), "Names"
      )
      val changeLogEntry = submitSource(
         """type FirstName inherits String
         |type LastName inherits String
         |model Person {
         |  firstName : FirstName
         |  lastName : LastName
         |}
      """.trimMargin(), packageName = "Names"
      )!!
      changeLogEntry.diffs.single().should.equal(
         ChangeLogDiffEntry(
            displayName = "Person",
            kind = DiffKind.ModelChanged,
            schemaMember = "Person".fqn(),
            children = listOf(
               ChangeLogDiffEntry(
                  displayName = "lastName",
                  kind = DiffKind.FieldAddedToModel,
                  schemaMember = "Person".fqn(),
               )
            )
         )
      )
   }

   @Test
   fun `verify changelog entry when field removed from model`() {
      submitSource(
         """type FirstName inherits String
         |type LastName inherits String
         |model Person {
         |  firstName : FirstName
         |  lastName : LastName
         |}
      """.trimMargin(), "Names"
      )
      val changeLogEntry = submitSource(
         """type FirstName inherits String
         |type LastName inherits String
         |model Person {
         |  firstName : FirstName
         |}
      """.trimMargin(), packageName = "Names"
      )!!
      changeLogEntry.diffs.single().should.equal(
         ChangeLogDiffEntry(
            displayName = "Person",
            kind = DiffKind.ModelChanged,
            schemaMember = "Person".fqn(),
            children = listOf(
               ChangeLogDiffEntry(
                  displayName = "lastName",
                  kind = DiffKind.FieldRemovedFromModel,
                  schemaMember = "Person".fqn(),
               )
            )
         )
      )
   }

   @Test
   fun `verify changelog entry when service added`() {
      submitSource(
         """type FirstName inherits String
         |type LastName inherits String
         |model Person {
         |  firstName : FirstName
         |  lastName : LastName
         |}
      """.trimMargin(), "Names"
      )
      val changeLogEntry = submitSource(
         """service People {
            |  operation findAllPeople():Person[]
            |}
      """.trimMargin(), packageName = "Services"
      )!!
      changeLogEntry.diffs.single().should.equal(
         ChangeLogDiffEntry(
            "People",
            DiffKind.ServiceAdded,
            "People".fqn(),
            listOf(
               ChangeLogDiffEntry(
                  "findAllPeople",
                  DiffKind.OperationAdded,
                  "People".fqn(),
               )
            )
         )
      )
   }

   @Test
   fun `verify changelog entry when service removed`() {
      submitSource(
         """type FirstName inherits String
         |type LastName inherits String
         |model Person {
         |  firstName : FirstName
         |  lastName : LastName
         |}
      """.trimMargin(), "Names"
      )
      submitSource(
         """service People {
            |  operation findAllPeople():Person[]
            |  operation findPerson(Int):Person
            |}
      """.trimMargin(), packageName = "Services"
      )!!
      val changeLogEntry = submitSource(
         "", packageName = "Services"
      )!!
      changeLogEntry.diffs.single().should.equal(
         ChangeLogDiffEntry(
            "People",
            DiffKind.ServiceRemoved,
            "People".fqn(),
         )
      )
   }

   @Test
   fun `verify changelog entry when operation added`() {
      submitSource(
         """type FirstName inherits String
         |type LastName inherits String
         |model Person {
         |  firstName : FirstName
         |  lastName : LastName
         |}
      """.trimMargin(), "Names"
      )
      submitSource(
         """service People {
            |  operation findAllPeople():Person[]
            |}
      """.trimMargin(), packageName = "Services"
      )!!
      val changeLogEntry = submitSource(
         """service People {
            |  operation findAllPeople():Person[]
            |  operation findPerson(Int):Person
            |}
      """.trimMargin(), packageName = "Services"
      )!!
      changeLogEntry.diffs.single().should.equal(
         ChangeLogDiffEntry(
            "People",
            DiffKind.ServiceChanged,
            "People".fqn(),
            listOf(
               ChangeLogDiffEntry(
                  "findPerson",
                  DiffKind.OperationAdded,
                  OperationNames.qualifiedName("People", "findPerson")
               )
            )
         )
      )
   }

   @Test
   fun `verify changelog entry when operation removed`() {
      submitSource(
         """type FirstName inherits String
         |type LastName inherits String
         |model Person {
         |  firstName : FirstName
         |  lastName : LastName
         |}
      """.trimMargin(), "Names"
      )
      submitSource(
         """service People {
            |  operation findAllPeople():Person[]
            |  operation findPerson(Int):Person
            |}
      """.trimMargin(), packageName = "Services"
      )!!
      val changeLogEntry = submitSource(
         """service People {
            |  operation findAllPeople():Person[]
            |}
      """.trimMargin(), packageName = "Services"
      )!!
      changeLogEntry.diffs.single().should.equal(
         ChangeLogDiffEntry(
            "People",
            DiffKind.ServiceChanged,
            "People".fqn(),
            listOf(
               ChangeLogDiffEntry(
                  "findPerson",
                  DiffKind.OperationRemoved,
                  OperationNames.qualifiedName("People", "findPerson")
               )
            )
         )
      )
   }

   @Test
   fun `verify changelog entry when operation return type changed`() {
      submitSource(
         """type FirstName inherits String
         |type LastName inherits String
         |model Person {
         |  firstName : FirstName
         |  lastName : LastName
         |}
      """.trimMargin(), "Names"
      )
      submitSource(
         """service People {
            |  operation findPerson(Int):Person
            |}
      """.trimMargin(), packageName = "Services"
      )!!
      val changeLogEntry = submitSource(
         """service People {
            |  operation findPerson(Int):Person[]
            |}
      """.trimMargin(), packageName = "Services"
      )!!
      changeLogEntry.diffs.single().should.equal(
         ChangeLogDiffEntry(
            "People",
            DiffKind.ServiceChanged,
            "People".fqn(),
            listOf(
               ChangeLogDiffEntry(
                  "findPerson",
                  DiffKind.OperationReturnValueChanged,
                  OperationNames.qualifiedName("People", "findPerson"),
                  oldDetails = "Person".fqn(),
                  newDetails = "Person[]".fqn()
               )
            )
         )
      )
   }


   @Test
   fun `verify changelog entry when operation parameters type changed`() {
      submitSource(
         """type FirstName inherits String
         |type LastName inherits String
         |model Person {
         |  firstName : FirstName
         |  lastName : LastName
         |}
      """.trimMargin(), "Names"
      )
      submitSource(
         """service People {
            |  operation findPerson(Int):Person
            |}
      """.trimMargin(), packageName = "Services"
      )!!
      val changeLogEntry = submitSource(
         """service People {
            |  operation findPerson(Int,Int):Person
            |}
      """.trimMargin(), packageName = "Services"
      )!!
      changeLogEntry.diffs.single().should.equal(
         ChangeLogDiffEntry(
            "People",
            DiffKind.ServiceChanged,
            "People".fqn(),
            listOf(
               ChangeLogDiffEntry(
                  "findPerson",
                  DiffKind.OperationParametersChanged,
                  OperationNames.qualifiedName("People", "findPerson"),
                  oldDetails = listOf(
                     ChangeLogDiffFactory.ParameterDiff(
                        null,
                        PrimitiveType.INTEGER.toVyneQualifiedName()
                     )
                  ),
                  newDetails = listOf(
                     ChangeLogDiffFactory.ParameterDiff(null, PrimitiveType.INTEGER.toVyneQualifiedName()),
                     ChangeLogDiffFactory.ParameterDiff(null, PrimitiveType.INTEGER.toVyneQualifiedName()),
                  )
               )
            )
         )
      )
   }

   @Test
   fun `verify changelog entry when operation url has changed`() {
      submitSource(
         """type FirstName inherits String
         |type LastName inherits String
         |model Person {
         |  firstName : FirstName
         |  lastName : LastName
         |}
      """.trimMargin(), "Names"
      )
      submitSource(
         """service People {
            |  @HttpOperation(url = "http://localhost/foo", method="POST")
            |  operation findPerson(Int):Person
            |}
      """.trimMargin(), packageName = "Services"
      )!!
      val changeLogEntry = submitSource(
         """service People {
            |  @HttpOperation(url = "http://localhost/bar", method="GET")
            |  operation findPerson(Int):Person
            |}
      """.trimMargin(), packageName = "Services"
      )!!
      changeLogEntry.diffs.single().should.equal(
         ChangeLogDiffEntry(
            "People",
            DiffKind.ServiceChanged,
            "People".fqn(),
            listOf(
               ChangeLogDiffEntry(
                  "findPerson",
                  DiffKind.OperationMetadataChanged,
                  OperationNames.qualifiedName("People", "findPerson"),
                  oldDetails = listOf(Metadata(name = "HttpOperation".fqn(), params = mapOf("url" to "http://localhost/foo", "method" to "POST"))),
                  newDetails = listOf(Metadata(name = "HttpOperation".fqn(), params = mapOf("url" to "http://localhost/bar", "method" to "GET")))
               )
            )
         )
      )
   }

   private fun submitSource(source: String, packageName: String): ChangeLogEntry? {
      sourceManager.submitPackage(
         VersionedSource.sourceOnly(
            source
         ).asPackage(organisation = "com.test", name = packageName)
      )
      return changeLogService.changeLog.lastOrNull()
   }
}

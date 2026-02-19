# Vyne Project - Claude Code Instructions

## Build System

This project uses **Maven** as its build system, NOT Gradle.

### Common Build Commands

- **Compile the project:** `mvn compile`
- **Run tests:** `mvn test`
- **Build specific module:** `mvn compile -pl schema-server-core`
- **Clean and build:** `mvn clean install`
- **Skip tests:** `mvn install -DskipTests`

## Project Structure

This is a multi-module Maven project with the following key modules:
- `schema-server-core` - Core schema server functionality
- `taxiq-query-engine` - Core query engine
- `station` - The main shipped application
- `orbital-ui` - User interface components
- Other modules as needed

## Important Notes

- Always use Maven commands (`mvn`) instead of Gradle (`./gradlew`)
- The project is structured as a standard Maven multi-module project


## Terms
Vyne is the legacy name for this platform. It's still used heavily throughout the code.

## UI
There's a separate CLAUDE.md in orbital-ui/CLAUDE.md which covers conventions and tips for building the UI project

## General
Try to reproduce bugs with a test. All changes to taxiql-query-engine MUST have an accompanying test.
There's a significant number of tests to use as reference.

In general:
- Build a vyne instance

```kotlin
val (vyne,stub) = testVyne("""
// The schema goes here
""")

stub.addResponse("operationName", """{ someJson }""")
```

## Code style
- Prefer writing new tests using DescribeSpec() and kotest assertions. We don't typically use infix for assertions

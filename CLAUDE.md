
 * The project uses maven for building the JVM codebase
 * docs are accessible in a separate repo, by convention at ../orbitalhq.com/ from the root of this project.

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

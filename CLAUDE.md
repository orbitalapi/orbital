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
- `orbital-ui` - User interface components
- Other modules as needed

## Important Notes

- Always use Maven commands (`mvn`) instead of Gradle (`./gradlew`)
- The project is structured as a standard Maven multi-module project

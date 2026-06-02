# Repository Guidelines

## Project Structure & Module Organization

This repository is a Java 21 Spring Boot application named `streamcell`.
Main application code lives in `src/main/java/com/streamcell`, with
`StreamcellApplication.java` as the bootstrap entry point. Runtime configuration
belongs in `src/main/resources`, currently `application.yaml`. Tests live in
`src/test/java/com/streamcell` and should mirror the package structure of the
main code. Gradle wrapper files (`gradlew`, `gradlew.bat`, `gradle/wrapper/`)
are checked in; use them instead of relying on a local Gradle install.

## Build, Test, and Development Commands

- `./gradlew bootRun` or `.\gradlew.bat bootRun`: run the Spring Boot app locally.
- `./gradlew test` or `.\gradlew.bat test`: run the JUnit Platform test suite.
- `./gradlew build` or `.\gradlew.bat build`: compile, test, and assemble the project.
- `./gradlew clean`: remove generated build output under `build/`.

On Windows PowerShell, prefer `.\gradlew.bat <task>`.

## Coding Style & Naming Conventions

Use standard Java formatting with 4-space indentation and braces on the same
line, matching the existing source. Keep packages under `com.streamcell`.
Name classes in `PascalCase`, methods and fields in `camelCase`, and constants
in `UPPER_SNAKE_CASE`. Prefer constructor injection for Spring components.
Lombok is available; use it only when it removes clear boilerplate without
hiding important behavior.

## Testing Guidelines

Tests use JUnit 5 through Gradle's `useJUnitPlatform()` configuration and Spring
Boot test support. Place tests under `src/test/java` with names ending in
`Tests` for application or integration tests, for example
`StreamcellApplicationTests`. Add focused unit tests for service logic and
Spring context or MVC tests for framework wiring. Always run `.\gradlew.bat test`
before opening a pull request.

## Commit & Pull Request Guidelines

Recent history uses short, imperative subjects such as `Initial commit` and
`Chore: docker-compose.yml`. Keep commit messages concise and describe the
change, using prefixes like `Chore:`, `Fix:`, or `Feature:` when helpful.
Pull requests should include a brief summary, the test command and result, any
configuration changes, and linked issues when applicable. Include screenshots
only for UI-facing changes.

## Security & Configuration Tips

Keep environment-specific settings out of source control. Store only safe
defaults in `application.yaml`; use environment variables or external profiles
for secrets, credentials, hostnames, and deployment-specific values.

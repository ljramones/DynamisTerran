# Repository Guidelines

## Project Structure & Module Organization
This repository is currently in an early scaffold state.
- `docs/`: project documentation and design notes.
- `.java-version`: Java toolchain pin (`25`) for local consistency.

As source code is added, keep a predictable layout:
- `src/main/java/` for production code
- `src/test/java/` for tests
- `docs/` for architecture decisions, setup notes, and API docs

## Build, Test, and Development Commands
No build system is committed yet (no `pom.xml` or `build.gradle`). Use these baseline checks:
- `java -version`: verify local JDK matches `.java-version`.
- `git status`: confirm a clean working tree before commits.
- `find docs -type f`: list all tracked documentation files.

When a build tool is introduced, document project-standard commands here (for example, `./mvnw test` or `./gradlew build`) and use wrapper scripts in CI.

## Coding Style & Naming Conventions
This project targets Java 25.
- Indentation: 4 spaces, no tabs.
- Types: `PascalCase` (for example, `TerrainGenerator`).
- Methods/fields: `camelCase`.
- Constants: `UPPER_SNAKE_CASE`.
- Packages: lowercase, dot-separated (for example, `com.dynamis.terrain`).

If formatting/lint tooling is added (such as Spotless or Checkstyle), treat it as required before merge.

## Testing Guidelines
There is no test framework configured yet. As tests are introduced:
- Place tests under `src/test/java`.
- Name tests with `*Test` suffix (for example, `HeightMapServiceTest`).
- Prefer deterministic unit tests; isolate filesystem/network dependencies.

Document exact test commands in this file once build tooling is committed.

## Commit & Pull Request Guidelines
Git history is not established yet, so use a clear starter convention:
- Commit format: `type(scope): summary` (for example, `feat(generator): add noise seed config`).
- Keep commits focused and atomic.
- Reference issue IDs in commit body or PR description.

PRs should include:
- Purpose and scope
- Validation steps run locally
- Screenshots or sample output for user-visible/documentation changes
- Linked issue/ticket when available

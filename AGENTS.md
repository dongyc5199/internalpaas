# Repository Guidelines

## Project Structure & Module Organization
- Java services live in `src/main/java/com/cmict/internalpaas`, grouped by feature packages such as `controller`, `service`, and `config`; shared DTOs and events stay under `dto` and `event`.
- Thymeleaf views are stored in `src/main/resources/templates`, while static assets (JS, CSS, images) belong in `src/main/resources/static`.
- Database change sets reside in `src/main/resources/db/migration`; operational scripts are in `scripts/`, documentation in `doc/`, and sample datasets in `data/`.
- Keep generated artifacts inside `target/` and out of version control.

## Build, Test, and Development Commands
- `./mvnw.cmd clean verify` builds the project, runs unit tests, and assembles the Spring Boot jar.
- `./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev` starts a local server with auto-reload; swap the profile to `prod` or `offline` when needed.
- `./mvnw.cmd -Poffline package` creates an offline distributable without network downloads.
- `scripts/download-vendor-libs.bat` (or the `.sh` variant) refreshes bundled vendor dependencies.

## Coding Style & Naming Conventions
- Target Java 17 with four-space indentation and lowercase package names.
- Prefer constructor injection and Lombok annotations such as `@RequiredArgsConstructor` to reduce boilerplate.
- Name Spring components with the standard suffixes (`*Controller`, `*Service`, `*Repository`) and Thymeleaf templates in kebab-case.
- Store configuration overrides in `application-<profile>.properties`.

## Testing Guidelines
- Tests live in `src/test/java` and use JUnit 5 through `spring-boot-starter-test`.
- Name test classes `<Feature>Test`; run a single suite via `./mvnw.cmd -Dtest=PasswordEncryptionFixTest test`.
- Prioritize coverage for security, encryption, and remote orchestration flows; add supporting fixtures under `data/` when relevant.

## Commit & Pull Request Guidelines
- Follow Conventional Commits, e.g. `feat(server-management): add remote reboot hook`; keep subjects imperative and under 60 characters. Scope nouns may be English or Chinese.
- Reference issue IDs where possible and keep each commit focused on one change.
- Pull requests should explain motivation, list verification steps, and attach UI screenshots when templates or static assets change.
- Ensure `./mvnw.cmd clean verify` passes before requesting review.

## Security & Configuration Tips
- Keep secrets in profile-specific property files and never commit production credentials.
- Review Flyway migrations and vendor scripts before promoting changes to shared environments.

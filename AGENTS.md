# Repository Guidelines

## Project Structure & Module Organization
The Spring Boot application code resides in `src/main/java/com/cmict/internalpaas`, organized by feature packages such as `controller`, `service`, and `config`, with shared DTOs and events grouped under `dto` and `event`. Thymeleaf templates and static assets live under `src/main/resources/templates` and `src/main/resources/static`, while Flyway migrations sit in `src/main/resources/db/migration`. Operational scripts for vendor asset management are in `scripts/`, documentation in `doc/`, and sample datasets in `data/`. Keep build outputs in `target/` out of version control.

## Build, Test, and Development Commands
Run `./mvnw.cmd clean verify` to compile, run unit tests, and produce the Spring Boot jar. Use `./mvnw.cmd spring-boot:run` for a local dev server with auto-reload; append `-Dspring-boot.run.profiles=dev` (or `prod`/`offline`) to match environment settings. Package for offline delivery with `./mvnw.cmd -Poffline package`, and refresh vendor bundles with `scripts\download-vendor-libs.bat` (use the `.sh` variant on Unix-like systems).

## Coding Style & Naming Conventions
Target Java 17 and four-space indentation. Follow Spring naming patterns (`*Controller`, `*Service`, `*Repository`) and keep package names lowercase. Prefer constructor injection, Lombok annotations such as `@RequiredArgsConstructor`, and immutable DTOs. Name templates with kebab-case, group static assets by type, and store configuration in `application-<profile>.properties`.

## Testing Guidelines
Tests live in `src/test/java`, rely on JUnit 5 and `spring-boot-starter-test`, and follow the `*Test` suffix (`CommandSecurityServiceTest`, `PasswordEncryptionFixTest`). Run the suite with `./mvnw.cmd test`; target a single class via `./mvnw.cmd -Dtest=PasswordEncryptionFixTest test`. Extend coverage for security, encryption, and remote server orchestration paths before merging.

## Commit & Pull Request Guidelines
Follow Conventional Commits as in `feat(服务器管理): …`; scope nouns may be English or Chinese but keep them concise. Use imperative subjects under 60 characters, reference issue IDs when available, and split large work into focused commits. Pull requests should state motivation, list verification steps, and include UI screenshots whenever templates or static assets change.

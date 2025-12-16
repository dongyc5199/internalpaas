# Repository Guidelines

## Project Structure & Module Organization
- `cmd/server`: Go entrypoint that wires configuration, middleware, routes, and the WebSocket hub.
- `internal/api/v1`: HTTP handlers; `internal/service` integrates Gitea, Drone, SonarQube, and Nexus; `internal/model` holds GORM entities.
- `internal/pkg`: shared helpers (`database`, `jwt`); `internal/websocket` manages real-time build logs; `internal/static` serves the built frontend.
- `config/config.yaml` defines runtime settings and reads secrets from `.env`; `.env.example` documents required values.
- `frontend/` is the Vite + React + TypeScript app (`src/components`, `pages`, `services`, `stores`, `hooks`, `contexts`, `types`, `utils`); production output lives in `internal/static/assets`.
- `deployments/`, `docker-compose.yml`, and `scripts/` hold infra/ops assets; `bin/` stores compiled binaries.

## Build, Test & Development Commands
- Prereqs: Go 1.22, Node 18+, Docker/Compose for the full stack.
- Backend: `make run` (dev server), `make build` (CGO=0 binary to `bin/codehub-server`), `make build-linux` (linux/amd64), `make up`/`make down` to start/stop all services, `make logs`/`make ps` for runtime status.
- Quality/security: `make fmt`, `make lint` (golangci-lint), `make vet`, `make vuln-check`.
- Frontend (from `frontend/`): `npm install`, `npm run dev`, `npm run build`, `npm run preview`, `npm run lint`.

## Coding Style & Naming Conventions
- Go: rely on `gofmt`; exported identifiers use `CamelCase`, internals use `lowerCamel`. Prefer structured `zap` logging, context-aware services, and thin handlers in `api/v1` delegating to `service`.
- TypeScript: strict mode is enabled in `tsconfig.json`; favor functional components and hooks, import paths via `@/` alias to `src`.
- Routing: keep RESTful verbs under `/api/v1/...`; mirror domain folders under `internal/`.

## Testing Guidelines
- Run `make test` for backend coverage and race checks (outputs `coverage.txt`); `make test-coverage` renders `coverage.html`.
- Place Go tests alongside code (`*_test.go`) and use table-driven cases where possible.
- Frontend currently relies on lint/type checks; run `npm run lint` and `npm run build` before submitting changes.

## Commit & Pull Request Guidelines
- Commit style follows recent history: `type(scope): summary` (e.g., `feat(frontend): add repository filters`), imperative mood, ~72-character subject.
- PRs should include what changed and why, validation steps (commands run), linked issues, and screenshots/GIFs for UI updates.
- Keep secrets in `.env`/`config/config.yaml` only; do not commit credentials or generated artifacts (`bin/`, `node_modules/`, coverage files).

---
name: angular-frontend-scaffold
description: Create and validate a deterministic Angular 21 frontend project with TypeScript, strict mode, SCSS, standalone APIs, a fixed development port, and a minimal functional shell. Use when starting the frontend of this monorepo. Do not use to add a feature to an existing Angular project.
---

# Angular frontend scaffold

Create a new, independent Angular frontend. The generator in this skill is the canonical way to create it; do not substitute hand-written `ng new` commands or alter its defaults.

## Required input

The project name is mandatory. If it was not supplied, ask exactly:

> Qual é o nome do projeto Angular que devo criar?

Do not create files, select a directory, or run the generator until the user provides the name.

## Deterministic defaults

| Item | Fixed value |
| --- | --- |
| Angular CLI | `@angular/cli@21` |
| Language | TypeScript with strict mode |
| Architecture | standalone components and lazy feature routes |
| Styling | SCSS + Tailwind CSS via PostCSS |
| Rendering | client-side rendering; no SSR |
| Zone mode | zoneless |
| Tests | no test files or test command |
| Development port | `4200`, explicit in `npm start` |
| API behavior | no server started and no request made during scaffold or validation |
| Initial verification | production build only |

Angular 21 is the baseline. A later Angular version may be used only when the user explicitly requests it and it is at least version 21.

## Workflow

1. Read [scaffold contract](references/scaffold-contract.md).
2. Confirm that the target directory does not already exist. Never overwrite an existing directory and never use `--force`.
3. Run:

   ```powershell
   node <skill>/scripts/scaffold.mjs --output <parent-directory> --project-name <project-name>
   ```

   Add `--angular-cli @angular/cli@<major>` only when the user explicitly chose a supported later major.
4. Review the generator output. It must include the minimal shell described in the contract, Tailwind CSS configured through `.postcssrc.json`, a feature-level `doc.md`, a typed HTTP boundary, and the explicit `start` script on port `4200`.
5. Validate only by running:

   ```powershell
   node <skill>/scripts/validate.mjs <generated-project>
   ```

6. Report the generated location and the build result. Do not run `npm test`, `ng test`, `ng serve`, `npm start`, a backend service, Docker, or HTTP requests.

## Boundaries

- This skill creates a frontend skeleton. It does not create feature modules beyond the functional home example.
- The shell registers `HttpClient` and a typed error boundary, but makes no API request. This guarantees that its build has no dependency on a running API.
- Use `angular-module-scaffold` only after this scaffold exists and an OpenAPI contract has been approved.
- Keep code minimal. Tailwind CSS is the only styling dependency installed by this scaffold. Do not add state libraries, UI component libraries, authentication, proxy configuration, SSR, tests, or environment-specific infrastructure at this stage.

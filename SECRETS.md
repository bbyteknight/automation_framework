# Secrets Management

No credentials, tokens, or API keys are ever committed to this repository.
All secrets are read at runtime from environment variables (or a local
`.env` file for development) via `config.ConfigReader`.

## How it works

`ConfigReader.get(key)` resolves values in this priority order:

1. JVM system property (`-Dkey=value`)
2. OS environment variable (`KEY=value`)
3. Local `.env` file (gitignored, dev-only — see `.env.example`)
4. `src/test/resources/config/{env}.properties` (non-secret config only)
5. Built-in default (non-secret config only)

Secret keys are **never** added to the `dev.properties` / `qa.properties` /
`staging.properties` files or to the built-in defaults — they must come from
steps 1–3 above, or `ConfigReader` throws rather than silently using a blank
value.

Lookup keys use camelCase in code (e.g. `ConfigReader.get("apiToken")`), which
is automatically converted to `SCREAMING_SNAKE_CASE` (`API_TOKEN`) when
checking environment variables and `.env`.

## Required secret names

| Key (env var / .env) | Used by | Description |
|---|---|---|
| `LOGIN_USERNAME` | Web — `LoginPage` / `LoginTest` | Web app login username |
| `LOGIN_PASSWORD` | Web — `LoginPage` / `LoginTest` | Web app login password |
| `API_TOKEN` | API — REST Assured clients | Bearer/API token for authenticated requests |
| `API_CLIENT_ID` | API — REST Assured clients | OAuth client ID, if applicable |
| `API_CLIENT_SECRET` | API — REST Assured clients | OAuth client secret, if applicable |
| `MOBILE_USERNAME` | Mobile — Appium tests | Mobile app login username |
| `MOBILE_PASSWORD` | Mobile — Appium tests | Mobile app login password |
| `BROWSERSTACK_USERNAME` | Mobile — `MobileDriverFactory`, `CloudAppUploader` (executionMode=cloud) | BrowserStack account username |
| `BROWSERSTACK_ACCESS_KEY` | Mobile — `MobileDriverFactory`, `CloudAppUploader` (executionMode=cloud) | BrowserStack account access key |

## Local setup

1. Copy the template: `cp .env.example .env`
2. Fill in real values in `.env` (this file is gitignored and never pushed).
3. Run tests normally — `ConfigReader` picks up `.env` automatically.

## CI / pipeline setup

Set the same key names as real environment variables / pipeline secrets
(e.g. GitHub Actions repository secrets, Azure DevOps variable groups marked
secret, Jenkins credentials bound to env vars). Do not add a `.env` file to
the pipeline — inject real environment variables instead so values never
touch disk in plaintext.

## Adding a new secret

1. Add the key to `.env.example` (left blank) so other contributors know it's required.
2. Add a row to the table above.
3. Read it via `ConfigReader.get("yourCamelCaseKey")` — do not read `System.getenv`
   directly, so the same priority/fallback behavior applies everywhere.

# E2E tests

Playwright tests to go through the Films demo.

## Getting started

Ensure that you've got the Films demo stack up and running with Docker Compose.

### Local development

Run

```shell
npm install
npx playwright test
```

### Docker Compose (easy to run)

Run the initialization command of the Films demo instruction to download the files (do not run `docker compose up`) and
run

```shell
docker build -t e2e .
cd demos/
# Download the demo files as per instructions
docker compose -f docker-compose.yml -f e2e.docker-compose.yml up --exit-code-from e2e
```

In case you want to run against a specific Vyne version, add `VYNE_VERSION=0.21.0-SNAPSHOT-BETA-a8ff0a5a` in front of
the whole up command.

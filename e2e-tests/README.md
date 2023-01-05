# E2E tests

These Playwright tests walk through the Films demo to see it still works as explained in the tutorial on our website.

## Getting started

There are two possible Vyne setups that one might want to use when running the tests:

1. Run against existing Docker images with Docker Compose. The limitation with this is that you need to have the Docker
   images available and as such it is not possible to run the tests against a local build of the Vyne components. Use
   this method if you want to ensure that any specific published version of Vyne works with the test setup.
2. By running query service and schema server locally. This is useful when developing the tests themselves or debugging
   issues on a branch that fails in CI.

Below you can find instructions for both methods.

### Docker Compose (easy to run)

Running the Docker Compose setup is easy and effortless:

```shell
docker build -t e2e .
cd demos/
# Download the demo files (with wget) as per the tutorial on our documentation website
docker compose -f docker-compose.yml -f e2e.docker-compose.yml up --exit-code-from e2e
```

In case you want to run against a specific Vyne version, add `VYNE_VERSION=0.21.0-SNAPSHOT-BETA-a8ff0a5a` in front of
the Docker Compose command.

### Local query service and schema server (for development)

1. The films demo configuration needs to be available. We use `local-development/` folder as a disposable container for
   these configuration files. The configuration can be obtained with:
   ```shell
   cd local-development/
   wget https://gitlab.com/vyne/demos/-/raw/master/films/docker-compose.yml
   wget https://gitlab.com/vyne/demos/-/raw/master/films/docker/schema-server/schema-server.conf -P vyne/schema-server
   wget https://gitlab.com/vyne/demos/-/raw/master/films/docker/schema-server/projects/taxi.conf -P vyne/schema-server/projects
   wget https://gitlab.com/vyne/demos/-/raw/master/films/services.conf -P vyne/config
   ```

   and by modifying the following files:
   - `docker-compose.yml`: Get rid of `vyne` and `schema-server` as those are run locally.
   - `schema-server.conf`: Change `file.projects[0].path` to point to
     the `<ABSOLUTE_PATH_TO_THIS_FOLDER>/local-development/vyne/schema-server/projects` (see `Important considerations`
     for the right format for Windows).
   - `services.conf`: Change `services.schema-server.url` to `localhost` instead of `schema-server`.

   The `local-development/` folder contents can be deleted whenever a fresh configuration is needed. E.g. to get rid of
   the schema the imported by the last run of the tests.

2. The query service and schema server need to be started locally. They need a few configuration options to point to the
   right configuration files.

   For query service:
   ```
   -Dvyne.repositories.config-file=<ABSOLUTE_PATH_TO_THIS_FOLDER>/local-development/vyne/schema-server/schema-server.conf
   ```

   For schema server:
   ```
   -Dvyne.services.config-file=<ABSOLUTE_PATH_TO_THIS_FOLDER>/local-development/vyne/config/services.conf
   -Dvyne.connections.configFile=<ABSOLUTE_PATH_TO_THIS_FOLDER>/local-development/vyne/config/connections.conf
   ```

3. We need to run the rest (excluding query service and schema server) of the Docker Compose setup:
   ```shell
   docker compose up
   ```

4. Finally, open another terminal and run the tests with:

   ```shell
   cd e2e-tests/
   npm install
   USE_LOCAL_HOSTNAMES=true npx playwright test --debug
   ```

   `--debug` is an optional option that will open a browser window and show the test execution visually and allows
   debugging the code along with exploring the DOM with Chrome developer tools.

## Important considerations

- The port used by the tests is `localhost:9022` by default. In case you want to run the tests against the automatically
  recompiled version of the UI (running in `4200`), you need to use `BASE_URL=http://localhost:4200` in front of the
  Playwright execution command.
- On Windows, using Git Bash, the absolute paths can be specified by starting with `/Users/<username>` (forward slashes
  and no need for the drive letter). E.g. `/Users/roope/Documents/code/vyne/vyne/e2e-tests/vyne/schema-server/projects`.

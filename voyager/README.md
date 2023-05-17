## Notes on the build

### Building & serving static content (the web app)

At dev time, use `ng build --watch` to build the angular app.

This outputs to `target/classes/static`, which Spring Boot will
automatically pick up at runtime.

#### Prod

In prod, we assemble using a different strategy, to keep build times
low.

Instead, the web app is built in parallel to the main java app.

Our docker build combines the two (see `Dockerfile`), and configrures
the spring boot app to serve from `opt/service/webapp`

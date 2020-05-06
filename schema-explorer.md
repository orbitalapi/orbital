# Running a simple schema explorer
The `docker-compose.yml` file deploys the Vyne UI
and a simple file server that publishes files
from the local filesystem.

A watcher runs to recompile and republish the files whenever
they change.

To run an environment which makes exploring schemas simple, do the following.

 * `mkdir schemas`
 * copy the schemas you want to explore there
 * `docker-compose up`
 * wait a bit
 * visit http://localhost:9022
 
Note, this also works with symlinked
folders:

```bash
ln -s /some/path/to/schemas ./schemas
```
 

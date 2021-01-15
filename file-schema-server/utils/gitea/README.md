This directory contains the neccessary steps to get a local
Gitea instance running.

This is useful for dev purposes to test committing and pushing 
against a git repo.

Note - this isn't required for unit tests.

# Starting Gitea
Just run:

```shell
docker-compose up
```

from this directory.

## Next:
 * Log in (http://localhost:3000) and create the db and a user account
 * Provide a ssh key
 * Create a repository

## Cloning repo / configuring in Vyne
Gitea is listening on a different port for ssh (222)

Therefore, you need to clone using the following syntax:

Assuming a user `martypitt` created a repo called `vyne-test`, then the syntax would be:

```shell
git clone ssh://git@localhost:222/martypitt/vyne-test.git
```


# Developing Orbital

Orbital is currently developed on Gitlab (for historic reasons), and mirrored
to Github.

This makes it difficult for us to accept contributions on Github.
Please reach out on [Slack](https://join.slack.com/t/orbitalapi/shared_invite/zt-697laanr-DHGXXak5slqsY9DqwrkzHg) if you'd like to contribute.

## Naming
Orbital used to be called Vyne, so you'll see that in many places throughout the code.
We're progressively moving this from Vyne to Orbital.

## Commit messages

Commit messages follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification that
allows them to be readable both by humans and machines easily.

The available types and scopes have been listed out in [.commitlintrc.json](.commitlintrc.json) and a plugin
called [Commitlint Conventional Commit](https://plugins.jetbrains.com/plugin/14046-commitlint-conventional-commit) for
IntelliJ IDEA adds a nice UI and checks the usage of the proper types and scopes when committing.

# Releasing

This repo uses a **tag-driven release** flow. You set the Maven project version, create a Git tag with the same version, and (optionally) push. Our CI will detect the tag and publish artifacts.

## Prerequisites

* Clean working tree (no uncommitted changes)
* Java + Maven available locally
* Ability to push to the default remote (`origin`)
* **Versioning rule:** **do not** prefix versions with `v` (use `1.2.3`, not `v1.2.3`)

## Release script

Use the `tag-release` helper script at the repo root:

```
./tag-release [-f] [-p] <release-version>
```

### What it does

1. Verifies your working tree is clean.
2. Ensures the tag `<release-version>` doesn’t already exist (unless `-f`).
3. Sets `project.version` in all POMs to `<release-version>` via `mvn versions:set`.
4. Runs the Maven Enforcer rule `requireReleaseDeps` to **fail** if any dependency is a `-SNAPSHOT`.
5. Commits the version bump.
6. Creates an annotated git tag `<release-version>` (or moves it if `-f`).
7. If `-p` is used, pushes the commit and tag to `origin`.

### Examples

* Local tag only (no push yet):

  ```
  ./tag-release 1.4.0
  ```

* Create and push:

  ```
  ./tag-release -p 1.4.0
  ```

* Move an existing tag to the new commit and push (use sparingly):

  ```
  ./tag-release -f -p 1.4.0
  ```

### CI Behavior

Pushing a tag triggers the pipeline. On tag pipelines, CI will:

* Build with the `release` profile
* Publish Maven artifacts (and Docker images where configured)

If CI fails, **do not** delete and recreate tags casually—prefer fixing the issue and re-running. If a tag must be moved, use `-f` **and** communicate with the team.

## Version Matching & Validation

* Tag **must exactly match** the Maven `project.version` (no `v` prefix).
* The script enforces:

   * No `v` prefix on the version you provide.
   * No `-SNAPSHOT` dependencies after the version bump.

## Troubleshooting

* **“Working tree has uncommitted changes”**
  Commit or stash changes before running the script.

* **“Tag already exists”**
  Use a new version or run with `-f` (only if you are absolutely sure).

* **Enforcer fails with SNAPSHOT deps**
  Update dependencies to released versions, or move SNAPSHOT usage behind non-release profiles. Re-run the script.

* **CI didn’t publish**
  Confirm the tag was pushed (`git push origin <tag>`). Check pipeline logs for failures.

## Rollback

If you created a tag/commit locally but haven’t pushed:

```
git tag -d 1.4.0
git reset --hard HEAD~1
```

If you already pushed (and truly need to undo), coordinate with the team:

```
git push --delete origin 1.4.0
# or, if moving a tag:
git tag -fa 1.4.0 -m "Move tag due to fix"
git push --force-with-lease origin refs/tags/1.4.0
```

---

Questions or edge cases? Ask in the team channel before force-moving tags.

What it does
Verifies your working tree is clean.

Ensures the tag <release-version> doesn’t already exist (unless -f).

Sets project.version in all POMs to <release-version> via mvn versions:set.

Runs the Maven Enforcer rule requireReleaseDeps to fail if any dependency is a -SNAPSHOT.

Commits the version bump.

Creates an annotated git tag <release-version> (or moves it if -f).

If -p is used, pushes the commit and tag to origin.

```bash
# Will create 
./tag-release 0.70.0
```

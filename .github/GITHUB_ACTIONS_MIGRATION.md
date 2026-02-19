# GitHub Actions Migration Guide

This document describes the migration from GitLab CI to GitHub Actions for the Orbital project.

## Overview

The GitLab CI configuration has been migrated to GitHub Actions with the following workflow files:

### Workflow Files

1. **`ci.yml`** - Main CI/CD Pipeline
   - Triggers: Push to all branches, tags, and pull requests
   - Jobs:
     - `verify-version`: Validates that tag versions match Maven POM version (tags only)
     - `build-jvm`: Builds Java/Maven projects
     - `build-orbital-ui`: Builds the Orbital UI
     - `build-playground-ui`: Builds the Playground UI
     - `publish-orbital`: Publishes Orbital Docker image (Alpine)
     - `publish-orbital-jammy`: Publishes Orbital Docker image (Ubuntu Jammy)
     - `publish-query-node`: Publishes Query Node Docker image
     - `tag-as-latest`: Tags release images as 'latest'

2. **`verify.yml`** - Verification and Security
   - Triggers: Push to develop branch, manual dispatch
   - Jobs:
     - `scan-orbital`: Trivy container security scanning
     - `validate-license-compliance`: License validation
     - `regression-test`: Manual regression testing

3. **`release.yml`** - Release Management
   - Triggers: Manual workflow dispatch
   - Jobs:
     - `release`: Creates major/minor/patch releases using Maven gitflow plugin

4. **`publish-core-types.yml`** - Publish Core Types
   - Triggers: Push to develop, master, and tags
   - Jobs:
     - `publish-core-types`: Publishes core types to GitHub repository

## Required Secrets

The following secrets need to be configured in GitHub repository settings (Settings → Secrets and variables → Actions):

### Required Secrets

1. **`DOCKER_HUB_PASSWORD`**
   - Description: Password for Docker Hub user 'vynecd'
   - Used for: Publishing Docker images to Docker Hub

2. **`GITHUB_PRIVATE_KEY`**
   - Description: SSH private key for accessing orbital-core-taxi repository
   - Used for: Publishing core types to GitHub

3. **`JOOQ_REPO_USERNAME`**
   - Description: Username for jOOQ Pro repository access
   - Used for: Maven build dependencies

4. **`JOOQ_REPO_PASSWORD`**
   - Description: Password for jOOQ Pro repository access
   - Used for: Maven build dependencies

### Optional Secrets

These may be needed depending on your Maven repository configuration:

- Maven repository credentials (if using private Maven repositories)
- Additional Docker registry credentials (if publishing to other registries)

## Key Differences from GitLab CI

### Branch Behavior

| Branch/Tag | GitLab CI | GitHub Actions |
|------------|-----------|----------------|
| Feature branches | `build-jvm-branch` - clean install | Same - clean install |
| `develop` | `build-jvm-develop` - clean deploy with `-P snapshot-release` | Same behavior |
| `release/*` | Deploy with `-P snapshot-release` | Same behavior |
| Tags (`v*`) | `build-jvm-release` - deploy with `-P release` | Same behavior |

### Docker Publishing

- **GitLab**: Used `jdrouet/docker-with-buildx` image
- **GitHub Actions**: Uses official Docker actions (setup-qemu-action, setup-buildx-action, build-push-action)

Docker tags follow the same pattern:
- `develop` → `next` and `next-{run_id}`
- `master` → `latest` and version
- Tags → tag name and tag name
- `release/*` → `{branch}-next` and `{version}-BETA-{run_id}`

### Caching

- **GitLab**: Explicit cache configuration for `.m2/repository` and `.npm/cache`
- **GitHub Actions**: Uses built-in cache actions (`actions/setup-java@v4` with `cache: maven`, `actions/setup-node@v4` with cache)

### Artifacts

- **GitLab**: 1-hour expiration for build artifacts
- **GitHub Actions**: 1-day retention for build artifacts, 7 days for test results

## Manual Workflows

### Creating a Release

In GitLab CI, releases were triggered manually on the develop branch with buttons. In GitHub Actions:

1. Go to Actions → Release workflow
2. Click "Run workflow"
3. Select branch: `develop`
4. Choose release type: `major`, `minor`, or `patch`
5. Click "Run workflow"

### Running Regression Tests

1. Go to Actions → Verify workflow
2. Click "Run workflow"
3. Select the branch you want to test
4. Click "Run workflow"

## Workflow Triggers

### Automatic Triggers

- **All branches**: Build and test on every push
- **develop, master, release/*, tags**: Build, test, and publish Docker images
- **develop**: Container scanning and license validation

### Manual Triggers

- **Release workflow**: Manual dispatch for creating releases
- **Verify workflow**: Manual dispatch for running verification tasks
- **Regression tests**: Part of verify workflow, manual dispatch only

## Migration Checklist

- [ ] Configure `DOCKER_HUB_PASSWORD` secret in GitHub
- [ ] Configure `GITHUB_PRIVATE_KEY` secret in GitHub
- [ ] Configure `JOOQ_REPO_USERNAME` secret in GitHub
- [ ] Configure `JOOQ_REPO_PASSWORD` secret in GitHub
- [ ] Verify Maven settings.xml is present at `.mvn/settings.xml`
- [ ] Test a feature branch build
- [ ] Test a develop branch build and Docker publish
- [ ] Verify container scanning works on develop
- [ ] Test manual release workflow (optional)
- [ ] Remove or archive `.gitlab-ci.yml` file

## Notes

1. **Multi-platform builds**: Both GitLab CI and GitHub Actions build for `linux/amd64` and `linux/arm64` platforms.

2. **Test reruns**: The `MAVEN_OPTS` includes `-Dsurefire.rerunFailingTestsCount=2` to automatically retry failing tests.

3. **Git depth**: Checkout uses `fetch-depth: 10` (same as GitLab's `GIT_DEPTH: 10`).

4. **Build numbers**: Uses `${{ github.run_id }}` instead of GitLab's `$CI_PIPELINE_ID`.

5. **Environment protection**: The `tag-as-latest` job could be protected with a GitHub environment for production deployments.

## Troubleshooting

### Docker build fails with authentication error
- Verify `DOCKER_HUB_PASSWORD` secret is correctly set
- Check that the password is for user 'vynecd'

### Maven build fails with dependency resolution
- Check that `.mvn/settings.xml` exists and is correctly configured
- Verify Maven cache is being used (check workflow logs)

### Publish core types fails
- Verify `GITHUB_PRIVATE_KEY` is correctly set
- Ensure the SSH key has access to the orbital-core-taxi repository
- Check that the key doesn't require a passphrase

### Container scan fails
- This is expected if there are HIGH or CRITICAL vulnerabilities
- Review the scan results in the Security tab
- Update dependencies to resolve vulnerabilities

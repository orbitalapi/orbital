#!/usr/bin/env bash
set -euo pipefail

###############################################################################
# tag-release
#
# Automates tagging and preparing a Maven release.
#
# Features:
#   • Validates that the target git tag doesn’t already exist (unless -f is used)
#   • Enforces that the version argument does NOT start with "v"
#   • Updates the Maven project version in pom.xml files using `mvn versions:set`
#   • Validates that no SNAPSHOT dependencies remain (after setting release version)
#   • Commits the version bump
#   • Creates a git tag (moving it if -f is given)
#   • Optionally pushes the commit and tag to origin
#
# Usage:
#   ./tag-release [-f] [-p] <release-version>
#
# Arguments:
#   <release-version>   The version to set in Maven and tag in Git (e.g., 1.2.3)
#
# Options:
#   -f    Force: allow moving an existing tag to the new commit
#   -p    Push:  push the commit and tag to origin after creating them
#
# Examples:
#   ./tag-release 1.2.3
#       Set Maven version to 1.2.3, commit, and create tag 1.2.3 locally
#
#   ./tag-release -p 1.2.3
#       Same as above, but also pushes commit and tag to origin
#
#   ./tag-release -f -p 1.2.3
#       Move existing tag 1.2.3 to the new commit and push with force
#
###############################################################################

usage() {
  echo "Usage: $0 [-f] [-p] <release-version>"
  echo "  -f   force: allow existing tag to be moved"
  echo "  -p   push:  push commit and tag to origin"
  exit 1
}

FORCE=false
PUSH=false

while getopts ":fp" opt; do
  case "$opt" in
    f) FORCE=true ;;
    p) PUSH=true ;;
    *) usage ;;
  esac
done
shift $((OPTIND-1))

[ $# -eq 1 ] || usage
VERSION="$1"

# Enforce that tag does NOT start with "v"
if [[ "$VERSION" == v* ]]; then
  echo "❌ Version '${VERSION}' starts with 'v'. Please remove the prefix to maintain consistency."
  exit 1
fi

# Ensure working tree is clean
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "❌ Working tree has uncommitted changes. Commit or stash first."
  exit 1
fi

# Check if tag exists
if git rev-parse -q --verify "refs/tags/${VERSION}" >/dev/null 2>&1; then
  if [ "$FORCE" = false ]; then
    echo "❌ Tag '${VERSION}' already exists. Use -f to move it."
    exit 1
  else
    echo "⚠️  Tag '${VERSION}' exists and will be moved (force)."
  fi
fi

# Set Maven version
echo "🔧 Setting Maven version to ${VERSION}"
mvn -q versions:set -DnewVersion="${VERSION}" -DgenerateBackupPoms=false

# Validate no snapshot dependencies AFTER bumping version
echo "🔍 Checking for SNAPSHOT dependencies..."
mvn -q org.apache.maven.plugins:maven-enforcer-plugin:3.5.0:enforce -Drules=requireReleaseDeps

# Commit the version bump
echo "📝 Committing version bump"
git add -A
git commit -m "chore(release): ${VERSION}"

# Create or move tag
if [ "$FORCE" = true ]; then
  git tag -fa "${VERSION}" -m "Release ${VERSION}"
else
  git tag -a  "${VERSION}" -m "Release ${VERSION}"
fi

# Push if requested
if [ "$PUSH" = true ]; then
  echo "🚀 Pushing to origin"
  git push origin HEAD
  if [ "$FORCE" = true ]; then
    git push --force-with-lease origin "refs/tags/${VERSION}"
  else
    git push origin "refs/tags/${VERSION}"
  fi
fi

echo "✅ Done. Created tag '${VERSION}'."

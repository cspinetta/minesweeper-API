#!/usr/bin/env bash

VERSION=$1

usage="${0} {version}"

function failureExit() {
  message=$1
  echo ${message}
  echo "Usage:"
  echo ${usage}
  exit 1
}

if [[ -z "$1" ]] ; then
  failureExit "Version not supplied."
fi

# Move to root
cd "$(dirname $0)"
set -e

echo "I'm checking if repository has changes"

if git diff-index --quiet HEAD --; then

    echo "Executing tests... "
    sbt clean test

    # Create tag
    echo "Deploying ${VERSION}"

    git push heroku ${VERSION}^{}:master
#    About the suffix ^{} it's to dereference the tag recursively until a non-tag object is found
#    More info at: http://disq.us/url?url=http%3A%2F%2Fschacon.github.io%2Fgit%2Fgitrevisions.html%3Axv6f9SjpbnDbg0PpDd1jr0H4iQQ&cuid=1188621

    echo "Done."
else
    echo "Project has uncommitted changes! commit them to continue, I'm guessing you don't want to deploy unversioned changes... or do you? D:"
    exit 1
fi

cd - > /dev/null
exit 0

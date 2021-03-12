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

    git push heroku ${VERSION}:master

    echo "Done."
else
    echo "Project has uncommitted changes! commit them to continue, I'm guessing you don't want to deploy unversioned changes... or do you? D:"
    exit 1
fi

cd - > /dev/null
exit 0

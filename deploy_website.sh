#!/bin/bash

# The website is built using MkDocs with the Material theme.
# https://squidfunk.github.io/mkdocs-material/
# It requires Python to run.
# Install the packages with the following command:
# pip install mkdocs mkdocs-material

set -ex

REPO="git@github.com:cashapp/sqldelight.git"
DIR=temp-clone-sqldelight

# Delete any existing temporary website clone
rm -rf $DIR

# Clone the current repo into temp folder
git clone $REPO $DIR

# Move working directory into temp folder
cd $DIR

# Copy in special files that GitHub wants in the project root.
cp UPGRADING.md docs/upgrading.md
cp CHANGELOG.md docs/changelog.md
cp CONTRIBUTING.md docs/contributing.md

# Build the site and push the new files up to GitHub
mkdocs gh-deploy

# Delete our temp folder
cd ..
rm -rf $DIR

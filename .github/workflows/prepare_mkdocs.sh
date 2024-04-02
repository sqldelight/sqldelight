#!/bin/bash

# The website is built using MkDocs with the Material theme.
# https://squidfunk.github.io/mkdocs-material/
# It requires Python to run.
# Install the packages with the following command:
# pip install --no-deps -r requirements.txt

set -ex

# Generate the API docs
./gradlew :dokkaHtmlMultiModule

# Fix up some styling/functionality on the generated dokka HTML pages
set +x
find docs/2.x/ -name '*.html' \
  -exec perl -i~ -0777 -pe 's/<a href="(.*)">([\n\s]+)?(<span>)?([\n\s]+)?SQLDelight([\n\s]+)?(<\/span>)?([\n\s]+)?<\/a>/<a href="\/sqldelight\/'"$1"'"><span>SQLDelight<\/span><\/a>/g' {} \; \
  -exec sed -i 's/<\/head>/<link rel="icon" href="\/sqldelight\/'"$1"'/images\/icon-cashapp.png"><\/head>/g' {} \;
set -x

# Copy in special files that GitHub wants in the project root.
cp CHANGELOG.md docs/changelog.md
cp CONTRIBUTING.md docs/contributing.md

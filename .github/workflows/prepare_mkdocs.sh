#!/bin/bash

# The website is built using MkDocs with the Material theme.
# https://squidfunk.github.io/mkdocs-material/
# It requires Python to run.
# Install the packages with the following command:
# pip install -r requirements.txt

set -ex

# Generate the API docs
./gradlew dokkaHtmlMultiModule

# Fix up some styling/functionality on the generated dokka HTML pages
set +x
for HTML_FILE in $(find docs/2.x/ -name '*.html'); do
  echo $HTML_FILE

  # Change header link to direct back to the main docs site
  sed -i '' 's/<a href="\(.*\)">SQLDelight<\/a>/<a href="\/sqldelight\/">SQLDelight<\/a>/g' $HTML_FILE
  sed -i '' 's/<a href="\(.*\)"><span>SQLDelight<\/span><\/a>/<a href="\/sqldelight\/"><span>SQLDelight<\/span><\/a>/g' $HTML_FILE
  # Add a link to the favicon
  sed -i '' 's/<\/head>/<link rel="icon" href="\/sqldelight\/images\/icon-cashapp.png"><\/head>/g' $HTML_FILE
done
set -x

# Copy in special files that GitHub wants in the project root.
cp CHANGELOG.md docs/changelog.md
cp CONTRIBUTING.md docs/contributing.md

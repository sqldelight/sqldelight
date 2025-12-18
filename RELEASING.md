# Releasing

1. Update the `VERSION_NAME` in `gradle.properties` to the release version.

2. Update the `CHANGELOG.md`:
    1. Change the `Unreleased` header to the release version and date.
    2. Add a link URL to ensure the header link works.
    3. Add a new `Unreleased` section to the top.

3. Commit

   ```
   $ git commit -am "Prepare version X.Y.Z"
   ```

4. Tag

   ```
   $ git tag -am "Version X.Y.Z" X.Y.Z
   ```

5. Update the `VERSION_NAME` in `gradle.properties` to the next "SNAPSHOT" version.

6. Commit

   ```
   $ git commit -am "Prepare next development version"
   ```

7. Push!

   ```
   $ git push && git push --tags
   ```

   This will trigger a GitHub Action workflow which will create a GitHub release and upload the
   release artifacts to Maven Central, NPM, and the IntelliJ IDEA plugin marketplace.

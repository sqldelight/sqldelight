Releasing
=========

 1. Change the version in `gradle.properties` to a non-SNAPSHOT verson.
 2. Update the `CHANGELOG.md` for the impending release.
 3. Update the `plugins.xml` change notes for the IDE plugin.
 4. Update the `README.md` with the new version.
 5. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
 6. `./gradlew clean uploadArchives`.
 7. Visit [Sonatype Nexus](https://oss.sonatype.org/) and promote the artifact.
 8. Visit the [JetBrains Plugin Portal](https://plugins.jetbrains.com/plugin/8191) and upload the IDE plugin zip.
 9. `git tag -a X.Y.X -m "Version X.Y.Z"` (where X.Y.Z is the new version)
 10. Update the `gradle.properties` to the next SNAPSHOT version.
 11. `git commit -am "Prepare next development version."`
 12. `git push && git push --tags`

If step 6 or 7 fails, drop the Sonatype repo, fix the problem, commit, and start again at step 5.

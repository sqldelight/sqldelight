Releasing
=========

 1. Change the version in `gradle.properties` and `mkdocs.yml` to a non-SNAPSHOT verson.
 2. Update the `CHANGELOG.md` for the impending release.
 3. Update the `README.md` with the new version.
 4. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
 5. `git tag -a X.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
 6. Update the `gradle.properties` to the next SNAPSHOT version.
 7. `git push && git push --tags`
 8. Wait until the "Publish a release" action completes, then visit [Sonatype Nexus](https://oss.sonatype.org/) and promote the artifacts.
 9. Update the sample app to the release version and send a PR.

If step 6 or 7 fails, drop the Sonatype repo, fix the problem, commit, and start again at step 5.

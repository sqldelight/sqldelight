Releasing
=========

 1. Update the `CHANGELOG.md` for the impending release.
 2. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
 3. `git tag -a X.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
 4. Update the `gradle.properties` to the next SNAPSHOT version.
 5. `git commit -am "Prepare next development version."`
 6. `git push && git push --tags`
 7. Wait until the "Publish a release" action completes, then visit [Sonatype Nexus](https://oss.sonatype.org/) and promote the artifacts.
 8. Update the sample app to the release version and send a PR.
 
If the github action fails, drop the artifacts from sonatype and re run the job.

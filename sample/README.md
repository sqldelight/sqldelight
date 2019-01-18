# Sample App

Android and iOS sample apps.

## Building iOS

You'll need Xcode installed and basic familiarity with building iOS apps before trying to run
this sample.

Run build on the command line

```
./gradlew build
```

Then open the Xcode project

```
cd sample/iosApp
open iosApp.xcodeproj
```

Select a simulator and run

The Xcode build *may* fail because it can't see ANDROID_HOME, in which case you can add
`sdk.dir` to local.properties
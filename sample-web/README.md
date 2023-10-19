# Web Sample App

This sample shows how to build a Kotlin/JS app with SQLDelight using the [web-worker-driver](https://cashapp.github.io/sqldelight/latest/js_sqlite/).
Unlike the mobile sample, this project uses SQLDelight's `generateAsync` mode to enable asynchronous execution with a SQLite database running in a Web Worker.

## Running the web sample

Launch the sample by running the following comand:

````shell
./gradlew :jsBrowserRun
````

The sample will be available at `http://localhost:8080`.

#Runs specific test. Pass in test name.
set -e
./gradlew :drivers:native-driver:linkDebugTestMingwX64
./drivers/native-driver/build/bin/mingwX64/debugTest/test.exe --ktest_regex_filter=.*$1.*

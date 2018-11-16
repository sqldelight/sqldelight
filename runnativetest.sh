#Runs specific test. Pass in test name.
./gradlew :drivers:ios-driver:linkTestDebugExecutableMacos
./drivers/ios-driver/build/bin/macos/test/debug/executable/test.kexe --ktest_regex_filter=.*$1.*

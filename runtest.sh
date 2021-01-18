#Runs specific test. Pass in test name.
./gradlew :drivers:native-driver:linkDebugTestMacosX64
./drivers/native-driver/build/bin/macosX64/debugTest/test.kexe --ktest_regex_filter=.*$1.*

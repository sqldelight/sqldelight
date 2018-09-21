#!/bin/bash

set -ex

# Install SDK license so Android Gradle plugin can install deps.
mkdir "$ANDROID_HOME/licenses" || true
echo "d56f5187479451eabf01fb78af6dfcb131a6481e" > "$ANDROID_HOME/licenses/android-sdk-license"

# Install the rest of tools (e.g., avdmanager)
sdkmanager tools

# Install the system image
sdkmanager "system-images;android-18;default;armeabi-v7a"

# Create and start emulator for the script. Meant to race the install task.
echo no | avdmanager create avd --force -n test -k "system-images;android-18;default;armeabi-v7a"
$ANDROID_HOME/emulator/emulator -avd test -no-audio -no-window &

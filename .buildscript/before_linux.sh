#!/bin/bash

set -ex

# Update tools so that --licenses works
yes | sdkmanager tools

# Install SDK license so Android Gradle plugin can install deps.
mkdir "$ANDROID_HOME/licenses" || true
yes | sdkmanager --licenses

# Install the system image
sdkmanager "system-images;android-18;default;armeabi-v7a"

# Create and start emulator for the script. Meant to race the install task.
echo no | avdmanager create avd --force -n test -k "system-images;android-18;default;armeabi-v7a"
$ANDROID_HOME/emulator/emulator -avd test -no-audio -no-window -gpu swiftshader_indirect &

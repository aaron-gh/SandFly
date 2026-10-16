### Bash script for building the Backtalk APKs, for phones and watches
###
### The following environment variables must be set before executing this script
###   ANDROID_SDK           # path to local copy of Android SDK

# For help in getting the correct version numbers of gradle, the gradle plugin,
# and Java, see the following:
# https://developer.android.com/build/releases/gradle-plugin#updating-gradle
# https://docs.gradle.org/current/userguide/compatibility.html

START_TIME=$SECONDS

echo "pwd: $(pwd)"
echo "ls"; ls

# Use these environment variables to get more info during the build
# GRADLE_DEBUG=--debug
# GRADLE_STACKTRACE=--stacktrace
echo "#### GRADLE_DEBUG: $GRADLE_DEBUG"
echo "#### GRADLE_STACKTRACE: $GRADLE_STACKTRACE"

if [[ -z "$ANDROID_SDK" ]]; then
  echo "#### The environment variable ANDROID_SDK is not set"
  exit 1
else
  echo "#### ANDROID_SDK: $ANDROID_SDK"
fi
echo

# Keep anything else in local.properties, such as gemini.api.key, and only add the SDK path.
if ! grep -q '^sdk\.dir=' local.properties 2>/dev/null; then
  echo "#### Add sdk.dir to local.properties"
  echo "sdk.dir=${ANDROID_SDK}" >> local.properties
fi
echo

if ! java -version; then
  echo "#### java command not found in PATH"
  exit 1
fi

if ! ./gradlew -version; then
  echo "#### gradlew failed to start"
  exit 1
fi

if [[ "$GRADLE_DEBUG" = "--debug" ]]; then
  echo "#### gradle buildEnvironment"
  ./gradlew buildEnvironment
  echo
  echo "#### gradle dependencies"
  ./gradlew dependencies
  echo
  echo "#### gradle properties"
  ./gradlew properties
  echo
fi

echo "#### gradlew $GRADLE_DEBUG $GRADLE_STACKTRACE assembleDebug"
./gradlew ${GRADLE_DEBUG} ${GRADLE_STACKTRACE} assembleDebug
BUILD_EXIT_CODE=$?
echo

if [[ $BUILD_EXIT_CODE -eq 0 ]]; then
  echo "#### find . -name *.apk"
  find . -name "*.apk"
  echo
fi

DURATION=$(( SECONDS - START_TIME ))
echo "#### Build took this many seconds: $DURATION"

exit $BUILD_EXIT_CODE   ### This should be the last line in this file
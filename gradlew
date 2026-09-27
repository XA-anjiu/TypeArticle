#!/bin/sh
#
# Gradle wrapper startup script (POSIX)
#

APP_HOME=$(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ ! -f "$CLASSPATH" ]; then
  echo "ERROR: gradle-wrapper.jar not found at $CLASSPATH" >&2
  exit 1
fi

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD=java
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
  "-Dorg.gradle.appname=gradlew" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain "$@"

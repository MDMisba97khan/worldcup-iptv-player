#!/usr/bin/env bash
# ---------------------------------------------------------------
# Gradle wrapper script for use with gradle wrapper
# ---------------------------------------------------------------

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
app_path=$0

# Need this for relative symlinks.
while [ -h "$app_path" ] ; do
    ls=`ls -ld "$app_path"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        app_path="$link"
    else
        app_path=`dirname "$app_path"`/"$link"
    fi
done

APP_HOME=`dirname "$app_path"`/..

# Use the wrapper jar
exec java $JAVA_OPTS -classpath "\"$APP_HOME/gradle/wrapper/gradle-wrapper.jar\"" org.gradle.wrapper.GradleWrapperMain "$@"

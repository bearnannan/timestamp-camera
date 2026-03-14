@echo off
echo ========================================
echo Create local.properties with JDK 17
echo ========================================
echo.

REM Create local.properties file
(
echo # Android SDK location
echo sdk.dir=C:/Users/WATCHARA MANADEE/AppData/Local/Android/Sdk
echo.
echo # JDK 17 location for Gradle 9.1.0
echo org.gradle.java.home=C:/Program Files/Android/Android Studio/jbr
echo.
echo # Optional: Use system JDK 17 if installed
echo # org.gradle.java.home=C:/Program Files/Java/jdk-17
) > local.properties

echo ✅ Created local.properties with JDK 17 path
echo.
echo Now try building:
echo ./gradlew.bat clean
echo ./gradlew.bat assembleDebug
echo.
pause

@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
set "ANDROID_HOME=C:\Users\pc\AppData\Local\Android\Sdk"
set "GRADLE_HOME=C:\Users\pc\.gradle\wrapper\dists\gradle-8.7-bin\gradle-8.7"
set "PATH=%JAVA_HOME%\bin;%GRADLE_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%"
"%GRADLE_HOME%\bin\gradle.bat" assembleDebug

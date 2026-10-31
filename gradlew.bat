@ECHO OFF
SETLOCAL
SET DIR=%~dp0
IF EXIST "%DIR%gradle\wrapper\gradle-wrapper.jar" (
  "%JAVA_HOME%\bin\java.exe" -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
) ELSE (
  ECHO Missing gradle\wrapper\gradle-wrapper.jar
  EXIT /B 1
)
ENDLOCAL

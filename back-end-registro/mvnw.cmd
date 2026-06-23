@echo off
set SCRIPT_DIR=%~dp0
java -jar "%SCRIPT_DIR%\.mvn\wrapper\maven-wrapper.jar" %*
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

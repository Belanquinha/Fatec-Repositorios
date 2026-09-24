@echo off
set SCRIPT_DIR=%~dp0
if not defined JAVA_HOME if exist "C:\Program Files\Java\jdk-25" set "JAVA_HOME=C:\Program Files\Java\jdk-25"
if exist "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.16-bin\5grr65jo27hi51sujmtcldfovl\apache-maven-3.9.16\bin\mvn.cmd" (
    "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.16-bin\5grr65jo27hi51sujmtcldfovl\apache-maven-3.9.16\bin\mvn.cmd" %*
    exit /b %ERRORLEVEL%
)
java -jar "%SCRIPT_DIR%\.mvn\wrapper\maven-wrapper.jar" %*
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%


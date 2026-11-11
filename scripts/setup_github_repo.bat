@echo off
setlocal enabledelayedexpansion

echo ====================================================================
echo             Pulse Music - GitHub Repository Setup
echo ====================================================================
echo Configure your GitHub repository to host Pulse Music updates.
echo ====================================================================
echo.

set /p REPO_URL="Enter your GitHub repository URL (e.g. https://github.com/manikandan-dev/PulseMusic.git): "
if "%REPO_URL%"=="" (
    echo [ERROR] Repository URL cannot be empty.
    pause
    exit /b 1
)

echo.
echo Setting git remote origin to %REPO_URL%...
git remote remove origin 2>nul
git remote add origin %REPO_URL%

echo.
echo Verifying remote...
git remote -v

echo.
echo Repository configured!
echo You can now push your initial commit or use 'publish_update_github.bat'
echo to publish updates.
echo ====================================================================
pause

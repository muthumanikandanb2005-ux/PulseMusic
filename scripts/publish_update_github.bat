@echo off
setlocal enabledelayedexpansion

echo ====================================================================
echo             Pulse Music - GitHub Update Publisher
echo ====================================================================
echo This script will publish an official Pulse Music release to GitHub.
echo Pulse Music will automatically discover this release in the background
echo and download the update on devices!
echo ====================================================================
echo.

set /p VERSION="Enter release version tag (e.g. v1.1.0): "
if "%VERSION%"=="" (
    echo [ERROR] Version tag cannot be empty.
    pause
    exit /b 1
)

echo.
echo [1/4] Checking git status...
git status -s

echo.
echo [2/4] Committing any pending changes...
git add -A
git commit -m "Release Pulse Music %VERSION%"

echo.
echo [3/4] Creating and pushing release tag %VERSION%...
git tag -f %VERSION%
git push origin --tags
git push origin HEAD

echo.
echo [4/4] Verifying GitHub Actions workflow...
echo Release tag %VERSION% has been pushed to GitHub!
echo GitHub Actions will now automatically compile PulseMusic-%VERSION%.apk
echo and create the release on your GitHub repository.
echo.
echo Devices running Pulse Music will detect this update in the background.
echo.
echo ====================================================================
echo               Pulse Music Update Published Successfully!
echo ====================================================================
pause

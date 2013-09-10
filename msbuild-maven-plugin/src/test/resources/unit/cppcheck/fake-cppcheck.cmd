@echo off

if exist %~dp0\Release rmdir /s /q %~dp0\Release
mkdir %~dp0\Release
echo FAKE FILE > %~dp0\Release\configurations-test.exe
echo FAKE FILE > %~dp0\Release\configurations-test.dll
echo FAKE FILE > %~dp0\Release\configurations-test.lib
if exist %~dp0\Debug rmdir /s /q %~dp0\Debug
mkdir %~dp0\Debug
echo FAKE FILE > %~dp0\Debug\configurations-test.exe
echo FAKE FILE > %~dp0\Debug\configurations-test.dll
echo FAKE FILE > %~dp0\Debug\configurations-test.lib

:printargs
if not "%1" == "" echo %1 && shift && goto printargs

@echo off
echo This is Fake CppCheck 

:printargs
if not "%1" == "" echo %1 && shift && goto printargs

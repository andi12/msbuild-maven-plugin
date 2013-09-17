@echo off

:printargs
if not "%1" == "" set /p=%1,< nul & set /p=%1,< nul 1>&2 & shift & goto :printargs

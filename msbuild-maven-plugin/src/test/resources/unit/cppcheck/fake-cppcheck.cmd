@echo off

:printargs
if not "%1" == "" echo %1 && echo %1 1>&2 && shift && goto :printargs

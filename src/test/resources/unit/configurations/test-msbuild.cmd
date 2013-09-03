@echo off
:loop
if not "%1" == "" echo %1 && shift && goto loop

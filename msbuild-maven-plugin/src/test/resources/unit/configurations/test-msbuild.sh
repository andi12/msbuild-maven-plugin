#!/bin/bash

SCRIPT_DIR=`dirname $0`
if [ -d $SCRIPT_DIR/Release ]; then
	rm -rf $SCRIPT_DIR/Release
fi
mkdir $SCRIPT_DIR/Release
echo FAKE FILE > $SCRIPT_DIR/Release/configurations-test.exe
echo FAKE FILE > $SCRIPT_DIR/Release/configurations-test.dll
echo FAKE FILE > $SCRIPT_DIR/Release/configurations-test.lib
if [ -d $SCRIPT_DIR/Debug ]; then
	rm -rf $SCRIPT_DIR/Debug
fi
mkdir $SCRIPT_DIR/Debug
echo FAKE FILE > $SCRIPT_DIR/Debug/configurations-test.exe
echo FAKE FILE > $SCRIPT_DIR/Debug/configurations-test.dll
echo FAKE FILE > $SCRIPT_DIR/Debug/configurations-test.lib

echo "$@"
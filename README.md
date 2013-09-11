msbuild-maven-plugin
====================

Maven 3 plugin for building Visual Studio solutions and projects with msbuild

Usage
=====

For detailed usage instruction see the project Wiki at: 
https://github.com/andi12/msbuild-maven-plugin/wiki

This simple example shows basic setup to build from a solution file. 
Place the following in the <build> <pluging> section of you POM and
use our 'exe' packaging.

```xml
    <plugin>
        <groupId>uk.org.raje.maven.plugins</groupId>
        <artifactId>msbuild-maven-plugin</artifactId>
        <version>0.1.0</version>
        <extensions>true</extensions>
        <configuration>
            <projectFile>${basedir}/hello-world.vcxproj</projectFile>
            <platforms>
                <platform>
                    <name>Win32</name>
                    <configurations>
                        <configuration>
                            <name>Release</name>
                        </configuration>
                        <configuration>
                            <name>Debug</name>
                        </configuration>
                    </configurations>
                </platform>
            </platforms>
        </configuration>
    </plugin>
```

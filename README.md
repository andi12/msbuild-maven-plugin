msbuild-maven-plugin
====================

Maven 3 plugin for building Visual Studio solutions and projects with msbuild.

In addition to building using msbuild the plugin also supports:

* Creating a version resource from information in your POM
* Running CppCheck (1.61) to perform static analysis of your code.
* Generating, building and executing unit tests using CxxTest (4.2.1, don't use 4.3)
* Generating a configuration file for the Sonar runner

Usage
=====

For detailed usage instruction see the project Wiki at: 
https://github.com/andi12/msbuild-maven-plugin/wiki

This simple example shows basic setup to build from a solution file. 
Place the following in the &lt;build&gt; &lt;plugins&gt; section of you POM and
use our 'exe' packaging.

```xml
    <plugin>
        <groupId>uk.org.raje.maven.plugins</groupId>
        <artifactId>msbuild-maven-plugin</artifactId>
        <version>0.5</version>
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

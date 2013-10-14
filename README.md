msbuild-maven-plugin
====================

Maven 3 plugin for building Visual Studio solutions and projects with msbuild.

Requirements:

* Tested with Visual Studio 2010 and 2012
* .NET framework v4.0 (for msbuild)

In addition to building using msbuild the plugin also supports:

* Creating a version resource from information in your POM
* Running CppCheck (1.61) to perform static analysis of your code.
* Generating, building and executing unit tests using CxxTest (4.2.1, don't use 4.3)
* Generating configuration files for the Sonar runner to ingest source, CppCheck and CxxTest results.


Usage
=====

For detailed usage instruction see the project Wiki at: 
https://github.com/andi12/msbuild-maven-plugin/wiki

The plugin is available from Maven Central see http://search.maven.org/#search|ga|1|a%3A%22msbuild-maven-plugin%22

This simple example shows basic setup to build from a solution file. 
Place the following in the &lt;build&gt; &lt;plugins&gt; section of you POM and
use our 'exe' packaging.

```xml
    <plugin>
        <groupId>uk.org.raje.maven.plugins</groupId>
        <artifactId>msbuild-maven-plugin</artifactId>
        <version>1.1</version>
        <extensions>true</extensions>
        <configuration>
            <projectFile>hello-world.vcxproj</projectFile>
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

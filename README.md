msbuild-maven-plugin
====================

Maven 3 plugin for building Visual Studio solutions and projects with msbuild.

Requirements:

* Tested with Visual Studio 2010 and 2012
* .NET framework v4.0 (for msbuild)

In addition to building using msbuild the plugin also supports:

* Creating a version resource from information in your POM
* Running static analysis tools:
    * CppCheck (1.61)
    * Vera++ (1.2.1)
* Generating, building and executing unit tests using CxxTest (4.2.1, don't use 4.3 due to Windows compilation issues)
* Generating configuration files for the Sonar runner to ingest source, CppCheck, Vera and CxxTest results.


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
        <version>1.2.1</version>
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


Changelog
=========

This section provides a summary of significant changes in each release. For all details see the commitlog.

1.2.1 (5-Nov-2013)

* Add support for a set of exclude patterns to be specified for CppCheck and Vera
* Change default Vera profile to 'default' from 'full'

1.2 (1-Nov-2013)

* Add support for Vera++ code style checker
* #8: CppCheck outputs results of --check-config to the Maven log
* #21: CPPCHECK_PATH environment variable can be used to configure the location of CppCheck
* #21: CXXTEST_HOME environment variable can be used to configure the home path for CxxTest
* #20: Test report files from CxxTest output to target/test-results instead of target/surefire-reports
* #18: Support for maven.test.failure.ignore which suppresses a build failure if tests fail.
* #18: Support for skipTests property which skips test execution.

1.1 (10-Oct-2013)

* #13: Sonar properties files now include links configured in the POM
* #6: VersionInfoMojo allows input file to be configured
* #5: VersionInfoMojo allows destination file to be specified

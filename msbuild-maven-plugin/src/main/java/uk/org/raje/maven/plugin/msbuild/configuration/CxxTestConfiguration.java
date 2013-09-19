/*
 * Copyright 2013 Andrew Everitt, Andrew Heckford, Daniele Masato
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.org.raje.maven.plugin.msbuild.configuration;

import java.io.File;
import java.util.List;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Configuration holder for cxxtest configuration values.
 */
public class CxxTestConfiguration
{
    
    /**
     * The name of the environment variable that can store the path to CxxTestGen
     */
    public static final String CXXTEST_HOME = "CXXTEST_HOME";
    
    /**
     * The CxxTest name to output on debug/information messages
     */
    public static final String CXXTEST_NAME = "CxxTest";

    /**
     * Get the configured value for skip 
     * @return the configured value or false if not configured
     */
    public final boolean skip()
    {
        return skip;
    }

    /**
     * Get the configured home directory for CxxTest
     * @return the home directory for CxxTest
     */
    public final File getCxxTestHome()
    {
        return cxxTestHome;
    }

    /**
     * Get the configured List of test targets
     * @return the List of test targets
     */
    public final List<String> getTestTargets()
    {
        return testTargets;
    }
    
    /**
     * Get the configured name prefix for the generated test reports
     * @return the report name prefix
     */
    public final String getReportNamePrefix()
    {
        return reportNamePrefix;
    }    
    
    /**
     * Get the file name for the generated test runner
     * @return the test runner file name
     */
    public final String getTestRunnerName()
    {
        return testRunnerName;
    }        
    
    
    /**
     * Get the regular expression defining which header files contain the tests to run. 
     * @return the test header regular expression
     */
    public final String getTestHeaderPattern()
    {
        return testHeaderPattern;
    }        

    /**
     * Set to true to skip CxxTest functionality.
     */
    @Parameter( 
            defaultValue = "false", 
            readonly = false )
    protected boolean skip = false; 

    /**
     * The home directory for CxxTest
     * Note: The property is not specified here as it doesn't work.
     * @see uk.org.raje.maven.plugin.msbuild.AbstractMSBuildPluginMojo#cxxTestHome 
     */
    @Parameter( 
            readonly = false, 
            required = false )
    protected File cxxTestHome;

    /**
     * The set of test targets (projects) to build. This is in fact a required parameter because the test harness will 
     * generate an executable for each target, which we will then need to execute to run the tests. We do not want to
     * enforce it here though, so tests can still be skipped manually without the need to specify dummy test targets.
     */
    @Parameter(
            readonly = false,
            required = false )
    protected List<String> testTargets;
    
    /**
     * The name prefix for the generated test reports (one for each platform/configuration/target combination) 
     */
    @Parameter(
            defaultValue = "cxxtest-report",
            readonly = false,
            required = false )
    protected String reportNamePrefix = "cxxtest-report";
    
    /**
     * The file name for the generated test runner (one for each target) 
     */
    @Parameter(
            defaultValue = "cxxtest-runner.cpp",
            readonly = false,
            required = false )
    protected String testRunnerName = "cxxtest-runner.cpp";

    /**
     * A pattern defining which header files contain the tests to run. This pattern is common to all targets.
     */
    @Parameter(
            defaultValue = "*Test.h",
            readonly = false,
            required = false )
    protected String testHeaderPattern = "*Test.h";
}

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
     * The name to output on debug/information messages
     */
    public static final String TOOL_NAME = "CxxTest";

    /**
     * The name of the environment variable that can store the path to the CxxTest home directory
     */
    public static final String HOME_ENVVAR = "CXXTEST_HOME";
    
    /**
     * The name of the property that can store the path to the CxxTest home directory
     */
    public static final String HOME_PROPERTY = "cxxtest.home";

    /**
     * The message to use when skipping CxxTest execution
     */
    public static final String SKIP_MESSAGE = "Skipping test";
    
    /**
     * The name of the boolean property that instructs us to skip test execution.
     */
    public static final String SKIP_TESTS_PROPERTY = "skipTests";
    
    /**
     * The name of the boolean property that instructs us to ignore test failures.
     */
    public static final String IGNORE_FAILURE_PROPERTY = "maven.test.failure.ignore";

    /**
     * Get the configured value for skip 
     * @return the configured value or false if not configured
     */
    public final boolean getSkip()
    {
        return skip;
    }

    /**
     * Set the configured value for skip
     * @param skip the new value
     */
    public final void setSkip( boolean skip )
    {
        this.skip = skip;
    }

    /**
     * Get the configured value for testFailureIgnore
     * @return true if failing tests should be ignored
     */
    public final boolean getTestFailureIgnore()
    {
        return testFailureIgnore;
    }

    /**
     * Set the configured value for testFailureIgnore
     * @param b the new value
     */
    public final void setTestFailureIgnore( boolean b )
    {
        testFailureIgnore = b;
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
     * Set CxxTest home directory
     * @param newCxxTestHome the new path to store, replaces any existing value
     */
    public final void setCxxTestHome( File newCxxTestHome )
    {
        cxxTestHome = newCxxTestHome;
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
     * Get the configured name for the generated test reports
     * @return the report name
     */
    public final String getReportName()
    {
        return reportName;
    }    
    

    /**
     * Get the configured filename of the template file to use to create the test runner
     * @return the template filename
     */
    public final File getTemplateFile()
    {
        return templateFile;
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
     * Set this to "true" to skip running tests, but still compile them. Its use is NOT RECOMMENDED, 
     * but quite convenient on occasion.
     */
    @Parameter( 
            property = SKIP_TESTS_PROPERTY,
            defaultValue = "false", 
            readonly = false )
    protected boolean skipTests = false; 

    /**
     * Set this to "true" to ignore a failure during testing. Its use is NOT RECOMMENDED, 
     * but quite convenient on occasion.
     */
    @Parameter( 
            property = IGNORE_FAILURE_PROPERTY,
            defaultValue = "false", 
            readonly = false )
    protected boolean testFailureIgnore = false; 

    /**
     * The home directory for CxxTest
     * Note: The property name specified here is only for documentation, this doesn't work and needs to be manually
     * fixed in {@link AbstractMSBuildPluginMojo}
     */
    @Parameter( 
            property = HOME_PROPERTY,
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
     * The name for the generated test reports (one for each platform/configuration/target combination) 
     */
    @Parameter(
            defaultValue = "cxxtest-report",
            readonly = false,
            required = false )
    protected String reportName = "cxxtest-report";
    
    /**
     * The filename of the template to use to generate the test runner.
     * <p>
     * To specify a template for each test project provide just the filename which will be found in each project 
     * directory.<br/>
     * To use a single template specify a filename relative to the pom or the full path to the file to use. 
     */
    @Parameter(
            readonly = false,
            required = false )
    protected File templateFile;
    
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

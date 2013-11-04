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
package uk.org.raje.maven.plugin.msbuild;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;
import uk.org.raje.maven.plugin.msbuild.configuration.CxxTestConfiguration;
import uk.org.raje.maven.plugin.msbuild.parser.VCProject;
import uk.org.raje.maven.plugin.msbuild.streamconsumers.StderrStreamToLog;
import uk.org.raje.maven.plugin.msbuild.streamconsumers.StdoutStreamToLog;

/**
 *  
 */
@Mojo( name = CxxTestRunnerMojo.MOJO_NAME, defaultPhase = LifecyclePhase.TEST )
public class CxxTestRunnerMojo extends AbstractMSBuildMojo
{
    /**
     * The name this Mojo declares, also represents the goal.
     */
    public static final String MOJO_NAME = "test";

    /**
     * The name of the directory created under 'target' where we store CxxTest report files.
     * Use the standard surefire-reports directory as the files are in that format.
     */
    public static final String REPORT_DIRECTORY = "test-reports";

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException
    {
        boolean wasExecutionSuccessful = true;
        
        if ( !isCxxTestEnabled( "runner execution", false ) )
        {
            return;
        }
        
        //Check if we need to skip the test execution ONLY, and output a predefined message that a CI system (for 
        // example, Jenkins) may pick up; when this option is enabled the tests are still built, so the build will fail
        // if the tests do not compile.
        if ( cxxTest.getSkipTests() )
        {
            getLog().info( TEST_SKIP_EXECUTION_MESSAGE );
            return;
        }

        validateCxxTestConfiguration();

        for ( String testTarget : cxxTest.getTestTargets() ) 
        {
            for ( BuildPlatform platform : platforms ) 
            {
                for ( BuildConfiguration configuration : platform.getConfigurations() )
                {
                    try 
                    {
                        VCProject vcProject = getParsedProject( testTarget, platform, configuration );
                        wasExecutionSuccessful &= executeCxxTestTarget( vcProject.getOutputDirectory(), testTarget, 
                                platform, configuration );
                    }
                    catch ( MojoExecutionException mee )
                    {
                        getLog().error( mee.getMessage() );
                        throw mee;
                    }
                    catch ( MojoFailureException mfe )
                    {
                        getLog().error( mfe.getMessage() );
                        throw mfe;
                    }
                }
            }
        }
        
        if ( ! wasExecutionSuccessful )
        {
            if ( cxxTest.getIgnoreTestFailure() )
            {
                getLog().warn( "Some tests failed to pass" );
            }
            else
            {
                throw new MojoFailureException( "Some tests failed to pass" );
            }
        }
        else
        {
            getLog().info( "All tests passed" );
        }
    }
    
    private static final String TEST_SKIP_EXECUTION_MESSAGE = "Tests are skipped.";
    
    private CommandLineRunner createCxxTestRunner( File directory, String testTargetName )
            throws MojoExecutionException
    {
        final File testTargetExec = new File( directory, testTargetName + ".exe" );
        CxxTestRunner cxxTestRunner = new CxxTestRunner( testTargetExec, getLog() );
        cxxTestRunner.setWorkingDirectory( directory );

        return cxxTestRunner;
    }
    
    private boolean executeCxxTestRunner( CommandLineRunner cxxTestRunner ) throws MojoExecutionException
    {
        try
        {
            return cxxTestRunner.runCommandLine() == 0;
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "I/O error while executing command line", ioe );
        }
        catch ( InterruptedException ie )
        {
            throw new MojoExecutionException( "Process interrupted while executing command line", ie );
        }
    }
    
    private void copyCxxTestReport( String testTargetName, BuildPlatform platform, BuildConfiguration configuration, 
            File sourceDirectory, File destDirectory ) throws MojoExecutionException
    {
        final String reportSuffix = cxxTest.getReportName() + "-" + testTargetName;
        final String reportName = reportSuffix + "-" + platform.getName() + "-" + configuration.getName();
        final File reportSource = new File ( sourceDirectory, reportSuffix + ".xml" );
        final File reportDest = new File ( destDirectory, reportName + ".xml" );
        
        try 
        {
            FileUtils.copyFile( reportSource, reportDest );
            FileUtils.forceDelete( reportSource );
        }
        catch ( IOException ioe )
        { 
            throw new MojoExecutionException( "Failed to move " + CxxTestConfiguration.TOOL_NAME + " report "
                    + reportSource + " to " + reportDest, ioe );
        }
    }    
    
    private boolean executeCxxTestTarget( File directory, String testTarget, BuildPlatform platform, 
            BuildConfiguration configuration ) 
            throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "Running " + CxxTestConfiguration.TOOL_NAME.toLowerCase() 
                + " tests for target " + testTarget 
                + ", platform=" + platform.getName() + ", configuration=" + configuration.getName() + "." );
        
        String testTargetName = new File ( testTarget ).getName();
        CommandLineRunner cxxTestRunner = createCxxTestRunner( directory, testTargetName );

        boolean wasExecutionSuccessful = executeCxxTestRunner( cxxTestRunner );
        copyCxxTestReport( testTargetName, platform, configuration, cxxTestRunner.getWorkingDirectory(),
                new File( mavenProject.getBuild().getDirectory(), REPORT_DIRECTORY ) );

        return wasExecutionSuccessful;
    }
    
    /**
     * Runs a given test target (executable generated by a Visual C++ test project) and produces a test report.
     */
    private static class CxxTestRunner extends CommandLineRunner
    {
        public CxxTestRunner( File testTargetExec, Log log )
        {
            super( CxxTestConfiguration.TOOL_NAME, new StdoutStreamToLog( log ), new StderrStreamToLog( log ) );
            this.testTargetExec = testTargetExec;
        }
        
        @Override
        protected List<String> getCommandLineArguments() 
        {
            return Arrays.asList( testTargetExec.getAbsolutePath() );
        }
        
        private File testTargetExec;
    }
    
}

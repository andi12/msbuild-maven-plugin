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
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.cli.StreamConsumer;

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
    public static final String REPORT_DIRECTORY = "surefire-reports";

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException
    {
        if ( !isCxxTestEnabled( "runner execution", false ) )
        {
            return;
        }

        if ( cxxTest.getSkipTests() )
        {
            getLog().info( "Tests are skipped." );
            return;
        }

        CXXTEST_RUNNER_LOG_HANDLER.setLog( getLog() );
        
        validateCxxTestConfiguration();
        List<Boolean> allTestPassed = new LinkedList<Boolean>();

        for ( String testTarget : cxxTest.getTestTargets() ) 
        {
            for ( BuildPlatform platform : platforms ) 
            {
                for ( BuildConfiguration configuration : platform.getConfigurations() )
                {
                    try 
                    {
                        VCProject vcProject = getParsedProject( testTarget, platform, configuration );
                        allTestPassed.add( executeCxxTestTarget( vcProject.getOutputDirectory(), testTarget, 
                                platform, configuration ) );
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
        
        if ( allTestPassed.contains( false ) )
        {
            if ( cxxTest.getTestFailureIgnore() )
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
    
    /**
     * This handler capture standard Java logging produced by {@link CxxTestRunner} and relays it to the Maven logger
     * provided by the Mojo. It needs to be static to prevent duplicate log output. 
     * @see {@link LoggingHandler#LoggingHandler(String name)} 
     */
    private static final LoggingHandler CXXTEST_RUNNER_LOG_HANDLER =
            new LoggingHandler( CxxTestRunner.class.getName() );    
    
    private CommandLineRunner createCxxTestRunner( File directory, String testTargetName )
            throws MojoExecutionException
    {
        File testTargetExec = new File( directory, testTargetName + ".exe" );
        
        CxxTestRunner cxxTestRunner = new CxxTestRunner( testTargetExec, 
                new StdoutStreamToLog( getLog() ), new StderrStreamToLog( getLog() ) );
        
        cxxTestRunner.setWorkingDirectory( directory );

        return cxxTestRunner;
    }
    
    private Boolean executeCxxTestRunner( CommandLineRunner cxxTestRunner ) throws MojoExecutionException
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
    
    private void moveCxxTestReport( String testTargetName, BuildPlatform platform, BuildConfiguration configuration, 
            File sourceDirectory, File destinationDirectory ) throws MojoExecutionException
    {
        String reportSuffix = cxxTest.getReportName() + "-" + testTargetName;
        File reportSource = new File ( sourceDirectory, reportSuffix + ".xml" );
        File reportDest = new File ( destinationDirectory, reportSuffix + "-" + platform.getName() + "-" 
                + configuration.getName() + ".xml" );
        
        try 
        {
            FileUtils.copyFile( reportSource, reportDest );
            FileUtils.forceDelete( reportSource );
        }
        catch ( IOException ioe )
        { 
            throw new MojoExecutionException( "Failed to move " + CxxTestConfiguration.CXXTEST_NAME + " report "
                    + reportSource + " to " + reportDest, ioe );
        }
    }    
    
    private Boolean executeCxxTestTarget( 
            File directory, String testTarget, 
            BuildPlatform platform, BuildConfiguration configuration ) 
            throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "Running " + CxxTestConfiguration.CXXTEST_NAME.toLowerCase() 
                + " tests for target " + testTarget 
                + ", platform=" + platform.getName() + ", configuration=" + configuration.getName() + "." );
        
        String testTargetName = new File ( testTarget ).getName();
        CommandLineRunner cxxTestRunner = createCxxTestRunner( directory, testTargetName );

        Boolean testPassed = executeCxxTestRunner( cxxTestRunner );
        moveCxxTestReport( testTargetName, platform, configuration, cxxTestRunner.getWorkingDirectory(), 
                getReportDirectory() );

        return testPassed;
    }
    
    private File getReportDirectory()
    {
        return new File( mavenProject.getBuild().getDirectory(), REPORT_DIRECTORY );
    }
    
    /**
     * Runs a given test target (executable generated by a Visual C++ test project) and produces a test report.
     */
    private class CxxTestRunner extends CommandLineRunner
    {
        public CxxTestRunner( File testTargetExec, StreamConsumer outputConsumer, StreamConsumer errorConsumer )
        {
            super( outputConsumer, errorConsumer );
            this.testTargetExec = testTargetExec;
        }
        
        @Override
        protected List<String> getCommandLineArguments() 
        {
            List<String> commandLineArguments = new LinkedList<String>();
            commandLineArguments.add( testTargetExec.getAbsolutePath() );

            return commandLineArguments;
        }
        
        private File testTargetExec;
    }
    
}

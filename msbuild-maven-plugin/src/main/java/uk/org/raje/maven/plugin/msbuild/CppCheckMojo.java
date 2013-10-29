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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;
import uk.org.raje.maven.plugin.msbuild.configuration.CppCheckConfiguration;
import uk.org.raje.maven.plugin.msbuild.parser.VCProject;
import uk.org.raje.maven.plugin.msbuild.streamconsumers.StdoutStreamToLog;

/**
 * Configure and run Cppcheck for static code analysis.
 */
@Mojo( name = CppCheckMojo.MOJO_NAME, defaultPhase = LifecyclePhase.VERIFY )
public class CppCheckMojo extends AbstractMSBuildPluginMojo 
{
    /**
     * The name this Mojo declares, also represents the goal.
     */
    public static final String MOJO_NAME = "cppcheck";

    /**
     * The name of the directory created under 'target' where we store CppCheck report files.
     */
    public static final String REPORT_DIRECTORY = "cppcheck-reports";

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException 
    {
        List<Boolean> allChecksPassed = new ArrayList<Boolean>();

        if ( !isCppCheckEnabled( false ) ) 
        {
            return;
        }
     
        validateCppCheckConfiguration();
        
        for ( BuildPlatform platform : platforms ) 
        {
            for ( BuildConfiguration configuration : platform.getConfigurations() )
            {

                for ( VCProject vcProject : getParsedProjects( platform, configuration, 
                        cppCheck.getExcludeProjectRegex() ) )
                {
                    getLog().info( "Running static code analysis for project " + vcProject.getName() + ", platform=" 
                            + vcProject.getPlatform() + ", configuration=" + vcProject.getConfiguration() );
                    
                    try 
                    {
                        allChecksPassed.add( runCppCheck( vcProject ) );
                    }
                    catch ( MojoExecutionException mee )
                    {
                        getLog().error( mee.getMessage() );
                        throw mee;
                    }
                }
            }
        }
        
        if ( allChecksPassed.contains( false ) )
        {
            throw new MojoFailureException( "Static code analysis failed" );
        }
        
        getLog().info( "Static code analysis complete" );
    }

    private String getSourcesForStdin( VCProject vcProject ) throws MojoExecutionException
    {
        StringBuilder stringBuilder = new StringBuilder();
        
        for ( File sourceFile : getProjectSources( vcProject, false ) )
        {
            try 
            {
                stringBuilder.append( getRelativeFile( vcProject.getBaseDirectory(), sourceFile ) + "\n" );
            }
            catch ( IOException ioe )
            {
                throw new MojoExecutionException( "Failed to compute relative path for file " + sourceFile, ioe );
            }
        }
        
        return stringBuilder.toString();
    }    
        
    private Writer createCppCheckReportWriter( File reportFile ) throws MojoExecutionException
    {
        BufferedWriter cppCheckReportWriter;
        
        try 
        {
            FileUtils.forceMkdir( reportFile.getParentFile() );
            cppCheckReportWriter = new BufferedWriter( new FileWriter( reportFile ) );
        } 
        catch ( IOException ioe ) 
        {
            throw new MojoExecutionException( "Failed to create " + CppCheckConfiguration.TOOL_NAME + " report " 
                    + reportFile, ioe );
        }

        return cppCheckReportWriter;
    }
    
    private CommandLineRunner createCppCheckRunner( VCProject vcProject, Writer reportWriter ) 
            throws MojoExecutionException
    {
        CppCheckRunner cppCheckRunner = new CppCheckRunner( cppCheck.getCppCheckPath(), reportWriter, getLog() );
        
        cppCheckRunner.setWorkingDirectory( vcProject.getBaseDirectory() );
        cppCheckRunner.setStandardInputString( getSourcesForStdin( vcProject ) );
        cppCheckRunner.setCppCheckType( cppCheck.getCppCheckType() );
        cppCheckRunner.setIncludeDirectories( getRelativeIncludeDirectories( vcProject ) );
        cppCheckRunner.setPreprocessorDefs( vcProject.getPreprocessorDefs() );
        
        return cppCheckRunner;
    }

    /**
     * Adjust the list of include paths to be relative to the projectFile directory 
     */
    private List<File> getRelativeIncludeDirectories( VCProject vcProject ) throws MojoExecutionException
    {
        final List<File> relativeIncludeDirectories = new ArrayList<File>();
        
        for ( File includeDir : vcProject.getIncludeDirectories() )
        {
            if ( includeDir.isAbsolute() )
            {
                relativeIncludeDirectories.add( includeDir );
            }
            else
            {
                try
                {
                    File absoluteIncludeDir = new File ( vcProject.getFile().getParentFile(), includeDir.getPath() );
                    relativeIncludeDirectories.add( getRelativeFile( vcProject.getBaseDirectory(), 
                            absoluteIncludeDir.getCanonicalFile() ) );
                }
                catch ( IOException ioe )
                {
                    throw new MojoExecutionException( "Failed to compute relative path for directroy " + includeDir, 
                            ioe );
                }
            }
        }
        
        return relativeIncludeDirectories;
    }

    private Boolean executeCppCheckRunner( CommandLineRunner cppCheckRunner ) 
        throws MojoExecutionException
    {
        try
        {
            return cppCheckRunner.runCommandLine() == 0;
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "I/O error while executing command line ", ioe );
        }
        catch ( InterruptedException ie )
        {
            throw new MojoExecutionException( "Process interrupted while executing command line ", ie );
        }
    }

    private void finaliseReportWriter( Writer reportWriter, File reportFile ) throws MojoExecutionException
    {
        try 
        {
            reportWriter.close();
        } 
        catch ( IOException ioe ) 
        { 
            throw new MojoExecutionException( "Failed to finalise " + CppCheckConfiguration.TOOL_NAME + " report" 
                    + reportFile, ioe );
        }
    }

    private Boolean runCppCheck( VCProject vcProject ) throws MojoExecutionException, MojoFailureException
    {
        File reportFile = getReportFile( vcProject );
        Writer reportWriter = createCppCheckReportWriter( reportFile );
        CommandLineRunner cppCheckRunner = createCppCheckRunner( vcProject, reportWriter );
        Boolean checksPassed = executeCppCheckRunner( cppCheckRunner );
        finaliseReportWriter ( reportWriter, reportFile );
        
        return checksPassed;
    }
    
    private File getReportFile( VCProject vcProject ) 
    {
        File reportDirectory = new File( vcProject.getFile().getParentFile(), REPORT_DIRECTORY );
        return new File( reportDirectory, cppCheck.getReportName() + "-" + vcProject + ".xml" );
    }
    
    private static class CppCheckRunner extends CommandLineRunner
    {
        /**
         * Construct the CppCheckRunner
         * @param cppCheckPath the path to CppCheck.exe
         * @param sourcePath the relative path from the working directory to the source files to check
         * @param outputConsumer StreamConsumer for stdout 
         * @param errorConsumer StreamConsumer for stderr
         */
        public CppCheckRunner( File cppCheckPath, Writer reportWriter, Log log )
        {
            super( new StdoutStreamToLog( log ), new WriterStreamConsumer( reportWriter ) );
            this.cppCheckPath = cppCheckPath;
            CPPCHECK_RUNNER_LOG_HANDLER.setLog( log );
        }
        
        public void setCppCheckType( CppCheckConfiguration.CppCheckType cppCheckType ) 
        {
            this.cppCheckType = cppCheckType;
        }

        public void setIncludeDirectories( List<File> includeDirectories ) 
        {
            this.includeDirectories = includeDirectories;
        }

        public void setPreprocessorDefs( List<String> preprocessorDefs ) 
        {
            this.preprocessorDefs = preprocessorDefs;
        }    
        
        @Override
        protected List<String> getCommandLineArguments() 
        {
            final String reportXMLVersion = "1";
            List<String> commandLineArguments = new LinkedList<String>();
            
            commandLineArguments.add( cppCheckPath.getAbsolutePath() );
            commandLineArguments.add( "--enable=" + cppCheckType.name() );
            
            for ( File includeDirectory : includeDirectories ) 
            {
                //WARNING: remove any trailing slashes from include paths, as CppCheck can fail if these are present;
                // using {@link File}s to wrap include paths is safe, whereas using {@link String}s may cause problems.
                commandLineArguments.add( "-I" );
                commandLineArguments.add( "\"" + includeDirectory + "\"" );
            }

            for ( File excludeDirectory : excludeDirectories ) 
            {
                commandLineArguments.add( "-i" );
                commandLineArguments.add( "\"" + excludeDirectory + "\"" );
            }

            for ( String preprocessorDef : preprocessorDefs ) 
            {
                commandLineArguments.add( "-D" + preprocessorDef );
            }
            
            commandLineArguments.add( "--file-list=-" );
            commandLineArguments.add( "--xml" );
            commandLineArguments.add( "--xml-version=" + reportXMLVersion );
            commandLineArguments.add( "--quiet" );
            
            return commandLineArguments;
        }
        
        /**
         * This handler capture standard Java logging produced by {@link CppCheckRunner} and relays it to the Maven 
         * logger provided by the Mojo. It needs to be static to prevent duplicate log output. 
         * @see {@link LoggingHandler#LoggingHandler(String name)} 
         */
        private static final LoggingHandler CPPCHECK_RUNNER_LOG_HANDLER = 
                new LoggingHandler( CppCheckRunner.class.getName() );
        
        private File cppCheckPath;
        private CppCheckConfiguration.CppCheckType cppCheckType = CppCheckConfiguration.CppCheckType.all;
        private List<File> includeDirectories = new LinkedList<File>();
        private List<File> excludeDirectories = new LinkedList<File>();
        private List<String> preprocessorDefs = new LinkedList<String>();
    }
    
}

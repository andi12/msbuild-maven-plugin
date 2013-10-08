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
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;
import uk.org.raje.maven.plugin.msbuild.configuration.CppCheckConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.CppCheckType;
import uk.org.raje.maven.plugin.msbuild.parser.VCProject;
import uk.org.raje.maven.plugin.msbuild.streamconsumers.StdoutStreamToLog;

/**
 * Configure and run Cppcheck static analysis tool.
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
        if ( !isCppCheckEnabled( false ) ) 
        {
            return;
        }
     
        CPPCHECK_RUNNER_LOG_HANDLER.setLog( getLog() );
        validateCppCheckConfiguration();
        
        List<Boolean> allChecksPassed = new LinkedList<Boolean>();
        
        Pattern projectExcludePattern = null;
        if ( cppCheck.getExcludeProjectRegex() != null )
        {
            projectExcludePattern = Pattern.compile( cppCheck.getExcludeProjectRegex() );
        }
        for ( BuildPlatform platform : platforms ) 
        {
            for ( BuildConfiguration configuration : platform.getConfigurations() )
            {

                for ( VCProject vcProject : getParsedProjects( platform, configuration, projectExcludePattern ) )
                {
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

    /**
     * Finds 'cppcheck-reports' directories and removes them.
     * @throws MojoFailureException
     */
    protected static void clean( File projectFile, Log log ) throws MojoFailureException
    {
        final DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setIncludes( new String[]{ "**\\" + REPORT_DIRECTORY } );
        directoryScanner.setBasedir( projectFile.getParentFile() );
        directoryScanner.scan();

        log.info( "Cleaning up " + CppCheckConfiguration.CPPCHECK_NAME + " reports" );
        
        for ( String directoryName : directoryScanner.getIncludedDirectories() )
        {
            final File directory = new File( projectFile.getParentFile(), directoryName );
            log.debug( "Deleting directory " + directory );
            
            try
            {
                FileUtils.deleteDirectory( directory );
            }
            catch ( IOException ioe )
            {
                log.error( "Failed to delete directory " + directory );
                throw new MojoFailureException( ioe.getMessage(), ioe );
            }
        }

        log.info( CppCheckConfiguration.CPPCHECK_NAME + " report clean-up complete" );
    }

    private static final String CPPCHECK_XML_VERSION = "1";
        
    /**
     * This handler capture standard Java logging produced by {@link CppCheckRunner} and relays it to the Maven logger
     * provided by the Mojo. It needs to be static to prevent duplicate log output. 
     * @see {@link LoggingHandler#LoggingHandler(String name)} 
     */
    private static final LoggingHandler CPPCHECK_RUNNER_LOG_HANDLER = 
            new LoggingHandler( CppCheckRunner.class.getName() );    

    /**
     * Runs CppCheck against a given Visual C++ project and produces a static code analysis report
     */    
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
            throw new MojoExecutionException( "Failed to create " + CppCheckConfiguration.CPPCHECK_NAME + " report " 
                    + reportFile, ioe );
        }

        return cppCheckReportWriter;
    }
    
    private CommandLineRunner createCppCheckRunner( VCProject vcProject, Writer reportWriter ) 
            throws MojoExecutionException
    {
        CppCheckRunner cppCheckRunner = new CppCheckRunner( cppCheck.getCppCheckPath(), 
                getRelativeFile( vcProject.getBaseDirectory(), vcProject.getFile().getParentFile() ), 
                new StdoutStreamToLog( getLog() ), new WriterStreamConsumer( reportWriter ) );
        
        cppCheckRunner.setWorkingDirectory( projectFile.getParentFile() );
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
        List<File> includeDirs = vcProject.getIncludeDirectories();
        List<File> result = new ArrayList<File>( includeDirs.size() );
        
        for ( File f : includeDirs )
        {
            if ( f.isAbsolute() )
            {
                result.add( f );
            }
            else
            {
                try
                {
                    File absF = new File ( vcProject.getFile().getParentFile(), f.getPath() ).getCanonicalFile();
                    result.add( getRelativeFile( projectFile.getParentFile(), absF ) );
                }
                catch ( IOException ioe )
                {
                    getLog().error( "Unexpected error calculating relative paths for include directories" );
                    throw new MojoExecutionException( ioe.getMessage() );
                }
            }
        }
        
        return result;
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
            throw new MojoExecutionException( "Failed to finalise " + CppCheckConfiguration.CPPCHECK_NAME + " report" 
                    + reportFile, ioe );
        }
    }

    private Boolean runCppCheck( VCProject vcProject ) throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "Running " + CppCheckConfiguration.CPPCHECK_NAME 
                + " static code analysis for project " + vcProject.getName() 
                + ", platform=" + vcProject.getPlatform() + ", configuration=" + vcProject.getConfiguration() + "." );

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
    
    private class CppCheckRunner extends CommandLineRunner
    {
        /**
         * Construct the CppCheckRunner
         * @param cppCheckPath the path to CppCheck.exe
         * @param sourcePath the relative path from the working directory to the source files to check
         * @param outputConsumer StreamConsumer for stdout 
         * @param errorConsumer StreamConsumer for stderr
         */
        public CppCheckRunner( File cppCheckPath, File sourcePath, StreamConsumer outputConsumer, 
                StreamConsumer errorConsumer )
        {
            super( outputConsumer, errorConsumer );
            this.cppCheckPath = cppCheckPath;
            this.sourcePath = sourcePath;
        }
        
        public void setCppCheckType( CppCheckType cppCheckType ) 
        {
            this.cppCheckType = cppCheckType;
        }

        public void setIncludeDirectories( List<File> includeDirectories ) 
        {
            this.includeDirectories = includeDirectories;
        }

        public void setExcludeDirectories( List<File> excludeDirectories ) 
        {
            this.excludeDirectories = excludeDirectories;
        }

        public void setPreprocessorDefs( List<String> preprocessorDefs ) 
        {
            this.preprocessorDefs = preprocessorDefs;
        }    
        
        @Override
        protected List<String> getCommandLineArguments() 
        {
            List<String> commandLineArguments = new LinkedList<String>();
            
            commandLineArguments.add( cppCheckPath.getAbsolutePath() );
            commandLineArguments.add( "--quiet" );
            commandLineArguments.add( "--xml" );
            commandLineArguments.add( "--xml-version=" + CPPCHECK_XML_VERSION );
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
            
            commandLineArguments.add( sourcePath.getPath() );
            
            return commandLineArguments;
        }
        
        private File cppCheckPath;
        private File sourcePath;
        private CppCheckType cppCheckType = CppCheckType.all;
        private List<File> includeDirectories = new LinkedList<File>();
        private List<File> excludeDirectories = new LinkedList<File>();
        private List<String> preprocessorDefs = new LinkedList<String>();
    }
    
}

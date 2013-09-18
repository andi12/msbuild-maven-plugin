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
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;
import org.xml.sax.SAXException;

import uk.org.raje.maven.plugin.msbuild.MojoHelper.LogOutputStreamConsumer;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;
import uk.org.raje.maven.plugin.msbuild.configuration.CppCheckConfiguration;
import uk.org.raje.maven.plugin.msbuild.parser.VCProject;
import uk.org.raje.maven.plugin.msbuild.parser.VCProjectParser;
import uk.org.raje.maven.plugin.msbuild.parser.VCSolutionParser;

/**
 * Configure and run Cppcheck static analysis tool.
 */
@Mojo( name = CppCheckMojo.MOJO_NAME, defaultPhase = LifecyclePhase.VERIFY )
public class CppCheckMojo extends AbstractCIToolsMojo 
{
    /**
     * The name this Mojo declares, also represents the goal.
     */
    public static final String MOJO_NAME = "cppcheck";
    
    /**
     * The message printed when the static code analysis generation is skipped.
     */
    public static final String CPPCHECK_SKIP_MESSAGE = "Skipping static code analysis";

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException 
    {
        List<VCProject> vcProjects = null;
        
        if ( !isCppCheckEnabled() ) 
        {
            return;
        }
        
        validateMojoConfiguration();
        
        getLog().info( "Performing static code analysis using " + CppCheckConfiguration.CPPCHECK_NAME + "." );
        
        for ( BuildPlatform platform : platforms ) 
        {
            for ( BuildConfiguration configuration : platform.getConfigurations() )
            {
                if ( MSBuildPackaging.isSolution( mavenProject.getPackaging() ) ) 
                {
                    vcProjects = processVCSolutionFile( platform, configuration );
                }
                else 
                {
                    vcProjects = processVCProjectFile( platform, configuration );
                }

                for ( VCProject vcProject : vcProjects )
                {
                    runCppCheck( vcProject, createCppCheckReportWriter( vcProject ) );
                }
            }
        }

        getLog().info( "Static code analysis complete." );
    }
    
    private boolean isCppCheckEnabled()
    {
        if ( cppCheck.skip() )
        {
            getLog().info( CPPCHECK_SKIP_MESSAGE + ", 'skip' set to true in the " + CppCheckConfiguration.CPPCHECK_NAME
                    + " configuration." );
            
            return false;
        }
        
        if ( cppCheck.cppCheckPath() == null ) 
        {
            getLog().info( CPPCHECK_SKIP_MESSAGE + ", path to " + CppCheckConfiguration.CPPCHECK_NAME + " not set." );
            return false;
        }        
        
        return true;
    }
    
    private void validateMojoConfiguration() throws MojoExecutionException, MojoFailureException
    {
        try 
        {
            MojoHelper.validateToolPath( cppCheck.cppCheckPath(), CppCheckConfiguration.CPPCHECK_PATH_ENVVAR, 
                    CppCheckConfiguration.CPPCHECK_NAME, getLog() );
        }
        catch ( FileNotFoundException fnfe )
        {
            throw new MojoExecutionException( CppCheckConfiguration.CPPCHECK_NAME + "could not be found at " 
                    + fnfe.getMessage() + ". "
                    + "You need to configure it in the plugin configuration section in the "
                    + "POM file using <cppCheckPath>...</cppCheckPath> "
                    + "or <properties><cppcheck.path>...</cppcheck.path></properties>; "
                    + "alternatively, you can use the command-line parameter -Dcppcheck.path=... "
                    + "or set the environment variable " + CppCheckConfiguration.CPPCHECK_PATH_ENVVAR, fnfe );
        }
        
        MojoHelper.validateProjectFile( mavenProject.getPackaging(), projectFile, getLog() );

        platforms = MojoHelper.validatePlatforms( platforms );
    }    
    
    private List<VCProject> processVCSolutionFile( BuildPlatform platform, BuildConfiguration configuration ) 
            throws MojoExecutionException
    {
        List<VCProject> vcProjects = null;
        vcProjects = parseVCSolution( platform, configuration );
        logVCSolutionConfiguration( platform, configuration );
        
        for ( VCProject project : vcProjects ) 
        {
            parseVCProject( project );
            logVCProjectConfiguration( project );
        }
        
        return vcProjects;
    }
    
    private List<VCProject> processVCProjectFile( BuildPlatform platform, BuildConfiguration configuration ) 
            throws MojoExecutionException
    {
        VCProject project = new VCProject( projectFile.getName(), projectFile );
        project.setPlatform( platform.getName() );
        project.setConfiguration( configuration.getName() );
        
        parseVCProject( project );
        logVCProjectConfiguration( project );
        
        return Arrays.asList( project );
    }
    
    private void logVCSolutionConfiguration( BuildPlatform platform, BuildConfiguration configuration ) 
    {
        getLog().info( "Solution " + projectFile.getName() + ": configuration '" + configuration.getName()
                + "', platform '" + platform.getName() + "'" );
    }
    
    private void logVCProjectConfiguration( VCProject project ) 
    {
        getLog().info( "Project " + project.getName() + ": configuration '" + project.getConfiguration() 
                + "', platform '" + project.getPlatform() + "'" );
        
        if ( project.getIncludeDirectories().size() > 0 ) 
        {
            getLog().info( "Project " + project.getName() + ": include directories " 
                    + project.getIncludeDirectories() );
        }
        
        if ( project.getPreprocessorDefs().size() > 0 ) 
        {
                getLog().info( "Project " + project.getName() + ": preprocessor definitions "
                        + project.getPreprocessorDefs() );
        }
    }
    
    private List<VCProject> parseVCSolution( BuildPlatform platform, BuildConfiguration configuration ) 
            throws MojoExecutionException
    {
        VCSolutionParser solutionParser;
        
        try 
        {
            solutionParser = new VCSolutionParser( projectFile, platform.getName(), configuration.getName(),
                    cppCheck.excludeProjectRegex() );
        }
        catch ( FileNotFoundException fnfe ) 
        {
            if ( projectFile == null ) 
            {
                throw new MojoExecutionException( "Missing projectFile", fnfe );
            }

            throw new MojoExecutionException( projectFile + " is not a valid solution file", fnfe );
        }
        
        try 
        {
            solutionParser.parse();
        }
        catch ( ParseException pe ) 
        {
            throw new MojoExecutionException( "Syntax error while parsing " + projectFile, pe );
        }
        catch ( IOException ioe ) 
        {
            throw new MojoExecutionException( "I/O error while parsing " + projectFile, ioe );
        }
        
        return solutionParser.getVCProjects();
    }
    
    private void parseVCProject( VCProject project ) 
            throws MojoExecutionException
    {
        VCProjectParser projectParser;
        
        try 
        {
            projectParser = new VCProjectParser( project.getPath(), project.getPlatform(), project.getConfiguration() );
        }
        catch ( FileNotFoundException fnfe ) 
        {
            throw new MojoExecutionException( "Missing project file " + project.getPath(), fnfe );
        }
        catch ( SAXException se ) 
        {
            throw new MojoExecutionException( "Syntax error while parsing " + project.getPath(), se );
        }
        catch ( ParserConfigurationException pce )
        {
            throw new MojoExecutionException( "XML parser configuration exception ", pce );
        }

        try 
        {
            projectParser.parse();
        }
        catch ( ParseException pe ) 
        {
            throw new MojoExecutionException( "Syntax error while parsing " + project.getPath(), pe );
        }
        catch ( IOException ioe ) 
        {
            throw new MojoExecutionException( "I/O error while parsing " + project.getPath(), ioe );
        }
        
        projectParser.updateVCProject( project );
    }
    
    private Writer createCppCheckReportWriter( VCProject vcProject ) throws MojoExecutionException
    {
        BufferedWriter cppCheckReportWriter;
        File cppCheckReport = new File( vcProject.getBaseDir(), cppCheck.reportName() + "-" 
                + vcProject.getPlatform() + "-" + vcProject.getConfiguration() + ".xml" );
        
        try 
        {
            cppCheckReportWriter = new BufferedWriter( new FileWriter( cppCheckReport ) );
        } 
        catch ( IOException ioe ) 
        {
            throw new MojoExecutionException( "Could not create " + CppCheckConfiguration.CPPCHECK_NAME + " report " 
                    + cppCheckReport, ioe );
        }

        return cppCheckReportWriter;
    }
    
    private void runCppCheck( VCProject vcProject, Writer cppCheckReportWriter ) throws MojoExecutionException
    {
        CppCheckRunner cppCheckRunner = new CppCheckRunner( cppCheck.cppCheckPath(), vcProject.getBaseDir(), 
                new LogOutputStreamConsumer( getLog() ), new WriterStreamConsumer( cppCheckReportWriter ) );
        
        cppCheckRunner.setCppCheckType( cppCheck.cppCheckType() );
        cppCheckRunner.setIncludeDirectories( vcProject.getIncludeDirectories() );
        cppCheckRunner.setPreprocessorDefs( vcProject.getPreprocessorDefs() );
        
        getLog().info( "Executing code analysis for project " + vcProject.getName() + "." );
        getLog().debug( "Executing command line " + cppCheckRunner.getCommandLine() );
        
        try
        {
            cppCheckRunner.runCommandLine();
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "I/O error while executing command line ", ioe );
        }
        catch ( InterruptedException ie )
        {
            throw new MojoExecutionException( "Process interrupted while executing command line ", ie );
        }
        
        try 
        {
            cppCheckReportWriter.close();
        } 
        catch ( IOException ioe ) 
        { 
            throw new MojoExecutionException( "Could not finalise " + CppCheckConfiguration.CPPCHECK_NAME + " report", 
                    ioe );
        }

        getLog().info( "Static code analysis for project " + vcProject.getName() + " succeeded." );
    }
}

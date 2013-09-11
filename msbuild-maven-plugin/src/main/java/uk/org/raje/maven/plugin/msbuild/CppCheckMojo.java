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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;
import org.xml.sax.SAXException;

import uk.org.raje.maven.plugin.msbuild.MojoHelper.LogOutputStreamConsumer;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;
import uk.org.raje.maven.plugin.msbuild.parser.VCProject;
import uk.org.raje.maven.plugin.msbuild.parser.VCProjectParser;
import uk.org.raje.maven.plugin.msbuild.parser.VCSolutionParser;

/**
 * @author dmasato
 *
 */
@Mojo( name = CppCheckMojo.MOJO_NAME, defaultPhase = LifecyclePhase.VERIFY )
public class CppCheckMojo extends AbstractCIToolsMojo 
{
    public static final String MOJO_NAME = "cppcheck";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException 
    {
        Collection<VCProject> vcProjects = null;
        
        if ( cppCheckPath == null ) 
        {
            getLog().debug( "Path to CppCheck not set. Skipping static code analysis..." );
            return;
        }
        
        validateMojoConfiguration();
        
        for ( BuildPlatform platform : platforms ) 
        {
            for ( BuildConfiguration configuration : platform.getConfigurations() )
            {
                if ( MojoHelper.isSolution( projectFile ) ) 
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
    }
    
    private void validateMojoConfiguration() throws MojoExecutionException, MojoFailureException
    {
        try 
        {
            MojoHelper.validateToolPath( cppCheckPath, CPPCHECK_PATH_ENVVAR, CPPCHECK_NAME, getLog() );
        }
        catch ( FileNotFoundException fnfe )
        {
            throw new MojoExecutionException( CPPCHECK_NAME + "could not be found at " + fnfe.getMessage() + ". "
                    + "You need to configure it in the plugin configuration section in the "
                    + "POM file using <cppCheckPath>...</cppCheckPath> "
                    + "or <properties><cppcheck.path>...</cppcheck.path></properties>; "
                    + "alternatively, you can use the command-line parameter -Dcppcheck.path=... "
                    + "or set the environment variable ", fnfe );
        }
        
        if ( platforms == null ) 
        {
            platforms = new ArrayList<BuildPlatform>();
            platforms.add( new BuildPlatform() );
        }
        
        MojoHelper.validatePlatforms( platforms );
        
        if ( cppCheckType == null ) 
        {
            cppCheckType = CppCheckType.all;
        }
    }    
    
    /**
     * The name of the environment variable that can store the location of CppCheck.
     */
    private static final String CPPCHECK_PATH_ENVVAR = "CPPCHECK_PATH";
    private static final String CPPCHECK_NAME = "CppCheck";

    private Collection<VCProject> processVCSolutionFile( BuildPlatform platform, BuildConfiguration configuration ) 
            throws MojoExecutionException
    {
        Collection<VCProject> vcProjects = null;
        vcProjects = parseVCSolution( platform, configuration );
        logVCSolutionConfiguration( platform, configuration );
        
        for ( VCProject project : vcProjects ) 
        {
            parseVCProject( project );
            logVCProjectConfiguration( project );
        }
        
        return vcProjects;
    }
    
    private Collection<VCProject> processVCProjectFile( BuildPlatform platform, BuildConfiguration configuration ) 
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
        if ( getLog().isInfoEnabled() ) 
        {
            getLog().info( "Solution " + projectFile.getName() + ": configuration '" + configuration.getName()
                    + "', platform '" + platform.getName() + "'" );
        }
    }
    
    private void logVCProjectConfiguration( VCProject project ) 
    {
        if ( getLog().isInfoEnabled() ) 
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
    }
    
    private Collection<VCProject> parseVCSolution( BuildPlatform platform, BuildConfiguration configuration ) 
            throws MojoExecutionException
    {
        VCSolutionParser solutionParser;
        
        try 
        {
            solutionParser = new VCSolutionParser( projectFile, configuration.getName(), platform.getName(), 
                    excludeProjectRegex );
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
            projectParser = new VCProjectParser( project.getPath(), project.getConfiguration(), project.getPlatform() );
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
        File cppCheckReport = new File( vcProject.getBaseDir(), reportName + "-" + vcProject.getPlatform() + "-" 
                + vcProject.getConfiguration() + ".xml" );
        
        try 
        {
            cppCheckReportWriter = new BufferedWriter( new FileWriter( cppCheckReport ) );
        } 
        catch ( IOException ioe ) 
        {
            throw new MojoExecutionException( "Could not create " + CPPCHECK_NAME + " report " + cppCheckReport, ioe );
        }

        return cppCheckReportWriter;
    }
    
    private void runCppCheck( VCProject vcProject, Writer cppCheckReportWriter ) throws MojoExecutionException
    {
        CppCheckRunner cppCheckRunner = new CppCheckRunner( cppCheckPath, vcProject.getBaseDir(), 
                new LogOutputStreamConsumer( getLog() ), new WriterStreamConsumer( cppCheckReportWriter ) );
        
        cppCheckRunner.setCppCheckType( cppCheckType );
        cppCheckRunner.setIncludeDirectories( vcProject.getIncludeDirectories() );
        cppCheckRunner.setPreprocessorDefs( vcProject.getPreprocessorDefs() );
        
        if ( getLog().isInfoEnabled() ) 
        {
            getLog().info( "Analysing project " + vcProject.getName() + "..." );
        }
        
        getLog().debug( "Executing command line: " + cppCheckRunner.getCommandLine() );
        
        try
        {
            cppCheckRunner.runCommandLine();
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( " I/O error while executing command line ", ioe );
        }
        catch ( InterruptedException ie )
        {
            throw new MojoExecutionException( " Process interrupted while executing command line ", ie );
        }
        
        try 
        {
            cppCheckReportWriter.close();
        } 
        catch ( IOException ioe ) 
        { 
            throw new MojoExecutionException( "Could not finalise " + CPPCHECK_NAME + " report", ioe );
        }

        if ( getLog().isInfoEnabled() ) 
        {
            getLog().info( "Done." );
        }
    }
    
    /**
     * The path to CppCheck.
     */
    @Parameter( property = "cppcheck.path", readonly = false, required = false )
    protected File cppCheckPath;

    @Parameter( defaultValue = "cppcheck-report", readonly = false, required = false )
    protected String reportName = "cppcheck-report";
    
    @Parameter( readonly = false, required = false )
    protected CppCheckType cppCheckType = CppCheckType.all;
    
    @Parameter( readonly = false,  required = false )
    protected String excludeProjectRegex;
}

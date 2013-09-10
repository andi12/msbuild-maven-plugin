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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.xml.sax.SAXException;

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
        validateMojoConfiguration();
        
        for ( BuildPlatform platform : platforms ) 
        {
            for ( BuildConfiguration configuration : platform.getConfigurations() )
            {
                if ( MojoHelper.isSolution( projectFile ) ) 
                {
                    processSolutionFile( platform, configuration );
                }
                else 
                {
                    processProjectFile( platform, configuration );
                }
            }
        }        
    }
    
    private void validateMojoConfiguration() throws MojoExecutionException, MojoFailureException
    {
        try 
        {
            MojoHelper.validateToolPath( cppCheckPath, ENV_CPPCHECK_PATH, "CppCheck", getLog() );
        }
        catch ( FileNotFoundException fnfe )
        {
            throw new MojoExecutionException( "CppCheck could not be found. "
                    + "You need to configure it in the plugin configuration section in the "
                    + "POM file using <cppCheckPath>...</cppCheckPath> "
                    + "or <properties><cppcheck.path>...</cppcheck.path></properties>; "
                    + "alternatively, you can use the command-line parameter -Dcppcheck.path=... "
                    + "or set the environment variable " + ENV_CPPCHECK_PATH, fnfe );
        }
        
        if ( platforms == null ) 
        {
            platforms = new ArrayList<BuildPlatform>();
            platforms.add( new BuildPlatform() );
        }
        
        MojoHelper.validatePlatforms( platforms );
    }    
    
    /**
     * The name of the environment variable that can store the location of CppCheck.
     */
    private static final String ENV_CPPCHECK_PATH = "CPPCHECK_PATH";    

    private void processSolutionFile( BuildPlatform platform, BuildConfiguration configuration ) 
            throws MojoExecutionException
    {
        Collection<VCProject> projects = null;
        projects = parseSolution( platform.getName(), configuration.getName() );
        getLog().info( "Solution " + projectFile + ": found configuration " + configuration.getName() + " for platform "
                + platform.getName() );
        
        for ( VCProject project : projects ) 
        {
            parseProject( project );
            logProjectConfiguration( project );
        }
    }
    
    private void processProjectFile( BuildPlatform platform, BuildConfiguration configuration ) 
            throws MojoExecutionException
    {
        VCProject project = new VCProject( projectFile.getName(), projectFile );
        project.setPlatform( platform.getName() );
        project.setConfiguration( configuration.getName() );
        
        parseProject( project );
        logProjectConfiguration( project );
    }
    
    private void logProjectConfiguration( VCProject project ) 
    {
        getLog().info( "Project " + project.getName() + ": found configuration " + project.getConfiguration() 
                + " for platform " + project.getPlatform() );
        getLog().info( "Project " + project.getName() + ": found include directories " + project.getIncludeDirs() );
        getLog().info( "Project " + project.getName() + ": found preprocessor defines " 
                + project.getPreprocessorDefs() );
    }
    
    private Collection<VCProject> parseSolution( String platformName, String configurationName ) 
            throws MojoExecutionException
    {
        VCSolutionParser solutionParser;
        
        try 
        {
            solutionParser = new VCSolutionParser( projectFile, configurationName, platformName, excludeProjectRegex );
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
        
        return solutionParser.getProjects();
    }
    
    private void parseProject( VCProject project ) 
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
        
        projectParser.updateProject( project );
    }
    

    @Parameter( readonly = false,  required = false )
    protected String excludeProjectRegex;
    
    /**
     * The path to CppCheck.
     */
    @Parameter( property = "cppcheck.path", readonly = false, required = true )
    protected File cppCheckPath;    
    
}

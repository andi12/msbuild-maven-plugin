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

package uk.org.raje.maven.plugin.msbuild.citools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.xml.sax.SAXException;

import uk.org.raje.maven.plugin.msbuild.citools.parser.ProjectParser;
import uk.org.raje.maven.plugin.msbuild.citools.parser.SolutionParser;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;

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
        Collection<Project> projects;
        
        validateCppCheckPath();
        projects = parseProjectFile();
    }
    
    private Collection<Project> parseProjectFile() throws MojoExecutionException, MojoFailureException 
    {
        Collection<Project> projects = null;

        for ( BuildPlatform platform : platforms ) 
        {
            for ( BuildConfiguration configuration : platform.getConfigurations() )
            {
                if ( isSolution() ) 
                {
                    projects = parseSolution( platform.getName(), configuration.getName() );
                    
                    for ( Project project : projects ) 
                    {
                        parseProject( project );
                    }
                }
                else 
                {
                    projects = new LinkedList<Project>();
                    Project project = new Project( projectFile.getName(), projectFile );
                    project.setPlatform( platform.getName() );
                    project.setConfiguration( configuration.getName() );
                    
                    parseProject( project );
                    projects.add( project );
                }
            }
        }
        
        return projects;
    }
    
    private Collection<Project> parseSolution( String platformName, String configurationName ) 
            throws MojoExecutionException
    {
        SolutionParser solutionParser;
        
        try 
        {
            solutionParser = new SolutionParser( projectFile, configurationName, platformName, excludeProjectRegex );
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
    
    private void parseProject( Project project ) 
            throws MojoExecutionException
    {
        ProjectParser projectParser;
        
        try 
        {
            projectParser = new ProjectParser( project.getPath(), project.getConfiguration(), project.getPlatform() );
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
    
    /**
     * Validates the path to CppCheck.
     * First looks at the Mojo configuration property, if not found there try the system environment.
     * @throws MojoExecutionException if CppCheck cannot be found
     */
    private void validateCppCheckPath() throws MojoExecutionException
    {
        if ( cppCheckPath == null )
        {
            // not set in configuration try system environment
            String cppCheckEnv = System.getenv( ENV_CPPCHECK_PATH );
            
            if ( cppCheckEnv != null )
            {
                cppCheckPath = new File( cppCheckEnv );
            }
        }
        
        if ( cppCheckPath != null
                && cppCheckPath.exists()
                && cppCheckPath.isFile() )
        {
            getLog().debug( "Using CppCheck at " + cppCheckPath );
        }
        else 
        {
            throw new MojoExecutionException(
                    "CppCheck could not be found. You need to configure it in the plugin configuration section in the "
                    + "POM file using <cppcheck.path>...</cppcheck.path> "
                    + "or <properties><cppcheck.path>...</cppcheck.path></properties>; "
                    + "alternatively, you can use the command-line parameter -Dcppcheck.path=... "
                    + "or set the environment variable " + ENV_CPPCHECK_PATH );
        }
    }    
    
    /**
     * The name of the environment variable that can store the location of CppCheck.
     */
    private static final String ENV_CPPCHECK_PATH = "CPPCHECK_PATH";
    

    @Parameter( readonly = false,  required = false )
    protected String excludeProjectRegex;
    
    /**
     * The path to CppCheck.
     */
    @Parameter( property = "cppcheck.path",
            readonly = false,
            required = true )
    protected File cppCheckPath;    
    
}

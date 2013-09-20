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
package uk.org.raje.maven.plugin.msbuild.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.xml.sax.SAXException;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;

/**
 * This class wraps project parsing functionality.
 */
public class VisualStudioProjectParser
{
    /**
     * Construct a project parser.
     * @param projectFile the solution or project file that will be read
     * @param logger the Log for this plugin instance
     */
    public VisualStudioProjectParser( File projectFile, Log logger )
    {
        this.projectFile = projectFile;
        this.log = logger;
    }

    public List<VCProject> projects()
    {
        return projects;
    }

    public void loadSolutionFile( BuildPlatform platform, BuildConfiguration configuration )
            throws MojoExecutionException 
    {
        projects = parseVCSolution( platform, configuration );
        logVCSolutionConfiguration( platform, configuration );
        
        for ( VCProject project : projects ) 
        {
            parseVCProject( project );
            logVCProjectConfiguration( project );
        }
    }

    public void loadProjectFile( BuildPlatform platform, BuildConfiguration configuration )
            throws MojoExecutionException 
    {
        VCProject project = new VCProject( projectFile.getName(), projectFile );
        project.setPlatform( platform.getName() );
        project.setConfiguration( configuration.getName() );
        
        parseVCProject( project );
        logVCProjectConfiguration( project );
        
        projects = Arrays.asList( project );
    }

    private void logVCSolutionConfiguration( BuildPlatform platform, BuildConfiguration configuration ) 
    {
        log.debug( "Solution " + projectFile.getName() + ": platform=" + platform.getName() 
                + ", configuration=" + configuration.getName() );
    }

    private void logVCProjectConfiguration( VCProject project ) 
    {
        log.debug( "Project " + project.getName() + ": platform=" + project.getPlatform() 
                + ", configuration=" + project.getConfiguration() );
        
        if ( project.getIncludeDirectories().size() > 0 ) 
        {
            log.debug( "Project " + project.getName() + ": include directories=" 
                    + project.getIncludeDirectories() );
        }
        
        if ( project.getPreprocessorDefs().size() > 0 ) 
        {
                log.debug( "Project " + project.getName() + ": preprocessor definitions="
                        + project.getPreprocessorDefs() );
        }
    }

    private List<VCProject> parseVCSolution( BuildPlatform platform, BuildConfiguration configuration )
            throws MojoExecutionException 
    {
        VCSolutionParser solutionParser;
        
        try 
        {
            solutionParser = new VCSolutionParser( projectFile, platform.getName(), configuration.getName() );
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

    private void parseVCProject( VCProject project ) throws MojoExecutionException 
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


    private Log log;
    private File projectFile;
    private List<VCProject> projects = new ArrayList<VCProject>();
}

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
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.xml.sax.SAXException;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;
import uk.org.raje.maven.plugin.msbuild.parser.VCParser;
import uk.org.raje.maven.plugin.msbuild.parser.VCProject;
import uk.org.raje.maven.plugin.msbuild.parser.VCProjectHandler;
import uk.org.raje.maven.plugin.msbuild.parser.VCSolutionHandler;

/**
 * This class wraps project parsing functionality.
 */
public class VCParserHelper
{
    public VCParserHelper( Log log )
    {
        this.log = log;
        vcParser = new VCParser( new VCSolutionLogger(), new VCProjectCollector() );
    }
    
    public List<VCProject> getVCProjects()
    {
        return vcProjects;
    }
    
    public void loadSolutionFile( File solutionFile, BuildPlatform platform, 
            BuildConfiguration configuration ) throws MojoExecutionException 
    {
        if ( solutionFile == null ) 
        {
            throw new MojoExecutionException( "Solution file not specified" );
        }

        vcProjects.clear();
        
        try 
        {
            vcParser.parseSolutionFile( solutionFile, platform.getName(), configuration.getName() );
        }
        catch ( FileNotFoundException fnfe ) 
        {
            throw new MojoExecutionException( "Could not find solution file " + solutionFile, fnfe );
        }
        catch ( IOException ioe ) 
        {
            throw new MojoExecutionException( "I/O error while parsing solution file " + solutionFile, ioe );
        }
        catch ( SAXException se ) 
        {
            throw new MojoExecutionException( "Syntax error while parsing solution file " + solutionFile, se );
        }
        catch ( ParserConfigurationException pce )
        {
            throw new MojoExecutionException( "XML parser configuration exception ", pce );
        }
        catch ( ParseException pe ) 
        {
            throw new MojoExecutionException( "Syntax error while parsing solution file " + solutionFile, pe );
        }
    }

    public void loadProjectFile( File projectFile, BuildPlatform platform, BuildConfiguration configuration ) 
            throws MojoExecutionException 
    {
        if ( projectFile == null ) 
        {
            throw new MojoExecutionException( "Project file not specified" );
        }
        
        vcProjects.clear();
        String name = projectFile.getName();
        
        if ( name.indexOf( '.' ) > 0 )
        {
            name = name.substring( 0, name.lastIndexOf( '.' ) );
        }
        
        try 
        {
            vcParser.parseProjectFile( projectFile, platform.getName(), configuration.getName() );
        }
        catch ( FileNotFoundException fnfe ) 
        {
            throw new MojoExecutionException( "Could not find project file " + projectFile, fnfe );
        }
        catch ( IOException ioe ) 
        {
            throw new MojoExecutionException( "I/O error while parsing project file " + projectFile, ioe );
        }
        catch ( SAXException se ) 
        {
            throw new MojoExecutionException( "Syntax error while parsing project file " + projectFile, se );
        }
        catch ( ParserConfigurationException pce )
        {
            throw new MojoExecutionException( "XML parser configuration exception ", pce );
        }
        catch ( ParseException pe ) 
        {
            throw new MojoExecutionException( "Syntax error while parsing project file " + projectFile, pe );
        }
    }
    
    private class VCSolutionLogger implements VCSolutionHandler
    {
        @Override
        public void parsedSolution( File solutionFile, String platform, String configuration ) 
        {
            log.debug( "Solution " + solutionFile.getName() + ": platform=" + platform 
                    + ", configuration=" + configuration );
        }
    }

    private class VCProjectCollector implements VCProjectHandler
    {
        @Override
        public void parsedProject( VCProject vcProject ) 
        {
            vcProjects.add( vcProject );
            
            log.debug( "Project " + vcProject.getName() + ": platform=" + vcProject.getPlatform() 
                    + ", configuration=" + vcProject.getConfiguration() );
            
            log.debug( "Project " + vcProject.getName() + ": output directory=" + vcProject.getOutputDirectory() );

            if ( vcProject.getIncludeDirectories().size() > 0 ) 
            {
                log.debug( "Project " + vcProject.getName() + ": include directories=" 
                        + vcProject.getIncludeDirectories() );
            }
            
            if ( vcProject.getPreprocessorDefs().size() > 0 ) 
            {
                log.debug( "Project " + vcProject.getName() + ": preprocessor definitions=" 
                        + vcProject.getPreprocessorDefs() );
            }
        }
    }
    
    private Log log;
    private VCParser vcParser;
    private List<VCProject> vcProjects = new ArrayList<VCProject>();
}

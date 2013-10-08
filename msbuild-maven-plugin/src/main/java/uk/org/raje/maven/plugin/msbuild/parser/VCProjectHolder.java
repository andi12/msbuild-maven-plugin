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
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
/**
 * This class represents a container for parsed Visual C++ projects. It is the entry point for using parsing 
 * functionalities in {@link in uk.org.raje.maven.plugin.msbuild.parser}. The container is populated as needed (lazy 
 * loading) using the {@link getParsedProjects} method.
 */
public final class VCProjectHolder
{
    /**
     * Find or create a container for parsed Visual C++ projects. If a container has already been created for the given
     * {@code inputFile} that container is returned; otherwise a new container is created. 
     * @param inputFile the file to parse, it can be a Visual Studio solution (containing Visual C++ projects) or a 
     * standalone Visual C++ project
     * @param isSolution {@code true} if {@code inputFile} is a solution, {@code false} if it is a standalone project
     * @return the container for parsed Visual C++ projects
     */
    public static VCProjectHolder getVCProjectHolder( File inputFile, boolean isSolution )
    {
        VCProjectHolder vcProjectHolder = VCPROJECT_HOLDERS.get( inputFile );
        
        if ( vcProjectHolder == null )
        {
            vcProjectHolder = new VCProjectHolder( inputFile, isSolution );
            VCPROJECT_HOLDERS.put( inputFile, vcProjectHolder );
        }
        
        return vcProjectHolder;
    }
    
    /**
     * Create a container for parsed Visual C++ projects.
     * @param inputFile the file to parse, it can be a Visual Studio solution (containing Visual C++ projects) or a 
     * standalone Visual C++ project
     * @param isSolution {@code true} if {@code inputFile} is a solution, {@code false} if it is a standalone project  
     */
    protected VCProjectHolder( File inputFile, boolean isSolution )
    {
        this.inputFile = inputFile;
        this.isSolution = isSolution;
    }
    
    /**
     * Return a {@link List} of {@link VCProject} beans for a given platform/configuration pair. Each {@link VCProject} 
     * bean holds properties for a parsed Visual C++ project.
     * @param platform the platform to use for parsing (for example, {@code Win32}, {@code x64})
     * @param configuration the configuration to use for parsing (for example,{@code Release}, {@code Debug})
     * @return a {@link List} of {@link VCProject}s that hold properties for parsed Visual C++ projects
     * @throws IOException if the input file does not exists or cannot be accessed, see 
     * {@link VCProjectHolder#VCProjectHolder(inputFile, isSolution)} 
     * @throws ParserConfigurationException  if a parser cannot be created which satisfies the requested configuration
     * @throws ParseException if an error occurs during parsing
     * @throws SAXException if a SAX parsing error occurs
     */
    public List<VCProject> getParsedProjects( String platform, String configuration ) 
            throws IOException, ParserConfigurationException, ParseException, SAXException
    {
        String key = platform + "-" + configuration;
        List<VCProject> vcProjects = parsedVCProjects.get( key );
        
        if ( vcProjects == null )
        {
            if ( isSolution ) 
            {
                vcProjects = parseVCSolution( inputFile, platform, configuration );
            }
            else 
            {
                vcProjects = Arrays.asList( parseStandaloneVCProject( inputFile, platform, configuration ) );
            }
            
            parsedVCProjects.put( key, vcProjects );
        }

        return vcProjects;
    }

    private static final Map< File, VCProjectHolder > VCPROJECT_HOLDERS = new HashMap<File, VCProjectHolder>();
    private final Logger logger = Logger.getLogger( getClass().getName() );
    
    private List<VCProject> parseVCSolution( File solutionFile, String platform, String configuration ) 
            throws IOException, ParserConfigurationException, ParseException, SAXException
    {
        String name = getFilename( solutionFile );
        
        logger.fine( "Parsing solution " + name + " with platform=" + platform 
                + ", configuration=" + configuration );
        
        VCSolutionParser vcSolutionParser = new VCSolutionParser( solutionFile, platform, configuration );
        vcSolutionParser.parse();
        
        for ( VCProject vcProject : vcSolutionParser.getVCProjects() ) 
        {
            logger.fine( "Solution " + name + ": found project " + vcProject.getName() 
                    + " with platform=" + vcProject.getPlatform() + ", configuration=" + vcProject.getConfiguration() );
            }

        logger.fine( "Solution parsing complete" );

        for ( VCProject vcProject : vcSolutionParser.getVCProjects() ) 
        {
            parseVCProject( vcProject, solutionFile );
        }
        
        return vcSolutionParser.getVCProjects();
    }

    private VCProject parseStandaloneVCProject( File projectFile, String platform, String configuration )
            throws IOException, ParserConfigurationException, ParseException, SAXException
    {
        
        VCProject vcProject = new VCProject( getFilename( projectFile ), projectFile, platform, configuration );
        parseVCProject( vcProject, null );
        
        return vcProject;
    }
    
    private String getFilename( File file )
    {
        String name = file.getName();
        
        if ( name.indexOf( '.' ) > 0 )
        {
            name = name.substring( 0, name.lastIndexOf( '.' ) );
        }
        
        return name;
    }
    
    private void parseVCProject( VCProject vcProject, File solutionFile ) 
        throws IOException, ParserConfigurationException, ParseException, SAXException
    {
        VCProjectParser vcProjectParser;
        File projectFile = vcProject.getFile();
        String name = vcProject.getName();
        
        logger.fine( "Parsing project " + name + " with platform=" + vcProject.getPlatform() 
                + ", configuration=" + vcProject.getConfiguration() );
        
        vcProjectParser = new VCProjectParser( projectFile, solutionFile, vcProject.getPlatform(), 
                vcProject.getConfiguration() );
        
        vcProjectParser.parse();
        vcProjectParser.updateVCProject( vcProject );
        
        logger.fine( "Project " + name + ": output directory=" + vcProject.getOutputDirectory() );

        if ( vcProject.getIncludeDirectories().size() > 0 ) 
        {
            logger.fine( "Project " + name + ": include directories=" + vcProject.getIncludeDirectories() );
        }
        
        if ( vcProject.getPreprocessorDefs().size() > 0 ) 
        {
            logger.fine( "Project " + name + ": preprocessor definitions=" + vcProject.getPreprocessorDefs() );
        }

        logger.fine( "Project parsing complete" );
    }
    
    private File inputFile;
    private boolean isSolution;
    
    /**
     * A Map containing {@link VCProject}s parsed from the project files for each platform-configuration pair.
     * The Map is populated as needed (lazy loading) by the method {@link #getParsedProjects}.
     */
    private Map<String, List<VCProject> > parsedVCProjects = new HashMap<String, List<VCProject>>();
}

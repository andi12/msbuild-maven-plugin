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

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * This class wraps project parsing functionality.
 */
public class VCParser
{
   
    public VCParser( VCSolutionHandler vcSolutionHandler, VCProjectHandler vcProjectHandler )
    {
        this.vcSolutionHandler = vcSolutionHandler;
        this.vcProjectHandler = vcProjectHandler;
    }
    
    public void parseSolutionFile( File solutionFile, String platform, String configuration ) 
            throws IOException, ParserConfigurationException, ParseException, SAXException
    {
        VCSolutionParser  vcSolutionParser = new VCSolutionParser( solutionFile, platform, configuration );
        vcSolutionParser.parse();
        vcSolutionHandler.parsedSolution( solutionFile, platform, configuration );
        
        for ( VCProject vcProject : vcSolutionParser.getVCProjects() ) 
        {
            parseVCProject( vcProject, solutionFile );
        }
    }

    public void parseProjectFile( File projectFile, String platform, String configuration )
            throws IOException, ParserConfigurationException, ParseException, SAXException
    {
        String name = projectFile.getName();
        
        if ( name.indexOf( '.' ) > 0 )
        {
            name = name.substring( 0, name.lastIndexOf( '.' ) );
        }
        
        VCProject vcProject = new VCProject( name, projectFile, platform, configuration );
        parseVCProject( vcProject, null );
        
        vcProjectHandler.parsedProject( vcProject );
    }

    private void parseVCProject( VCProject vcProject, File solutionFile ) 
        throws IOException, ParserConfigurationException, ParseException, SAXException
    {
        VCProjectParser vcProjectParser;
        File projectFile = vcProject.getProjectFile();
        
        vcProjectParser = new VCProjectParser( projectFile, solutionFile, vcProject.getPlatform(), 
                vcProject.getConfiguration() );
        
        vcProjectParser.parse();
        vcProjectParser.updateVCProject( vcProject );
        vcProjectHandler.parsedProject( vcProject );
    }
    
    private VCSolutionHandler vcSolutionHandler;
    private VCProjectHandler vcProjectHandler;
}

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * <p>Class that parses a Visual Studio C++ project. This class retrieves the following project properties:
 * <ul>
 *      <li>Include Directories (<em>i.e.</em> additional header locations).</li> 
 *      <li>Preprocessor Definitions (<em>i.e.</em> {@code #define}s used during code compilation such as {@code WIN32},
 *      {@code _DEBUG}).</li> 
 *      <li>Output Directory (location of the generated output file).</li>
 * </ul> 
 * These properties are necessary for other tools to work (for example, CppCheck, CxxTest, Sonar).</p>
 * <p>Once the C++ project has been parsed, the {@link VCProjectParser#updateVCProject} method can be used to update a 
 * {@link VCProject} bean with the values of the retrieved properties.
 */
class VCProjectParser extends BaseParser 
{
    /**
     * Create an instance of the Visual C++ project parser.
     * @param projectFile the Visual C++ project file to parse
     * @param solutionFile the solution file that contains this project if available, {@code null} 
     * otherwise
     * @param platform the platform for which to retrieve Visual C++ projects (for example, {@code Win32}, {@code x64})
     * @param configuration the configuration for which to retrieve Visual C++ projects (for example, {@code Release}, 
     * {@code Debug})
     * @throws FileNotFoundException if the given project file is not found
     * @throws ParserConfigurationException if a parser cannot be created which satisfies the requested configuration
     * @throws SAXException if a SAX parsing error occurs
     */
    public VCProjectParser( File projectFile, File solutionFile, String platform, String configuration ) 
            throws FileNotFoundException, ParserConfigurationException, SAXException 
    {
        super( projectFile, platform, configuration );
        SAXParserFactory factory = SAXParserFactory.newInstance();

        parser = factory.newSAXParser();
        this.solutionFile = solutionFile;
        
        //Assume the output directory is set to the default value. This can change later if the project specifies one
        outputDirectory = getDefaultOutputDirectory();
        
        //$(SolutionDir) is an absolute path and terminates with a separator
        envVariables.put( "SolutionDir", getBaseDirectory().getPath() + File.separator );
        envVariables.put( "Configuration", getConfiguration() );
        envVariables.put( "Platform", getPlatform() );
    }

    /**
     * Create an instance of the Visual C++ project parser.
     * @param projectFile the Visual C++ project file to parse (assume that no solution file is available)
     * @param platform the platform for which to retrieve Visual C++ projects (for example, {@code Win32}, {@code x64})
     * @param configuration the configuration for which to retrieve Visual C++ projects (for example, {@code Release}, 
     * {@code Debug})
     * @throws FileNotFoundException if the given project file is not found
     * @throws ParserConfigurationException if a parser cannot be created which satisfies the requested configuration
     * @throws SAXException if a SAX parsing error occurs
     */
    public VCProjectParser( File projectFile, String platform, String configuration ) 
            throws FileNotFoundException, ParserConfigurationException, SAXException 
    {
        this( projectFile, null, platform, configuration );
    }

    /**
     * Set the variable values to substitute while parsing the properties of this Visual C++ project (such as values 
     * for {@code SolutionDir}, {@code Platform}, {@code Configuration}, or for any environment variable expressed as 
     * {@code $(variable)} in the project properties); note that these values will <em>override</em> the defaults 
     * provided by the OS or set by {@link VCProjectHolder#getParsedProjects}
     * @param envVariables a map containing environment variable values to substitute while parsing the properties of
     * this Visual C++ project
     */
    public void setEnvVariables( Map<String, String> envVariables ) 
    {
        this.envVariables.putAll( envVariables );
    }
    
    /**
     * Update a {@link VCProject} bean with the Visual C++ project properties retrieved by the parser (Include 
     * Directories, Preprocessor Definitions and Output Directory).
     * @param vcProject the {@link VCProject} bean to update
     */
    public void updateVCProject( VCProject vcProject )
    {
        vcProject.setBaseDirectory( getBaseDirectory() );
        vcProject.setOutputDirectory( outputDirectory );
        vcProject.setPreprocessorDefs( preprocessorDefs );
        vcProject.setIncludeDirectories( includeDirs );
    }

    @Override
    public void parse() throws IOException, ParseException 
    {
        try 
        {
            parser.parse( getInputFile(), new VCProjectHandler() );
        }
        catch ( SAXParseException sape ) 
        {
            throw new ParseException( sape.getMessage(), sape.getLineNumber() );
        }
        catch ( SAXException sae ) 
        {
            throw new ParseException( sae.getMessage(), 0 );
        }
    }
    
    private static final List<String> PATH_PROPERTY_GROUP = Arrays.asList( "Project", "PropertyGroup" );
    private static final List<String> PATH_OUTDIR = Arrays.asList( "Project", "PropertyGroup", "OutDir" );
    private static final List<String> PATH_ITEM_DEFINITION_GROUP = Arrays.asList( "Project", "ItemDefinitionGroup" );
    
    private static final List<String> PATH_ADDITIONAL_INCDIRS = Arrays.asList( "Project", "ItemDefinitionGroup", 
            "ClCompile", "AdditionalIncludeDirectories" );
    
    private static final List<String> PATH_PREPROCESSOR_DEFS = Arrays.asList( "Project", "ItemDefinitionGroup", 
            "ClCompile", "PreprocessorDefinitions" );
    
    private class VCProjectHandler extends DefaultHandler
    {
        @Override
        public void startElement( String uri, String localName, String qName, Attributes attributes ) 
                throws SAXException 
        {
            path.add( qName );
            String condition = attributes.getValue( "Condition" );
            
            switch ( elementParserState ) 
            {
            case PARSE_PROPERTY_GROUP:
                
                //If there is no Condition attribute in the current element (which is a child inside a <ProperyGroup>
                // element), we assume that the Condition attribute was present in the parent <ProperyGroup> and that 
                // the Condition matched the required platform/configuration; otherwise we need to check whether the  
                // current element satisfies the required platform/configuration pair through the Condition attribute. 
                if ( condition == null
                    || ( condition != null && condition.contains( getConfigurationPlatform() ) ) )
                {
                    if ( path.equals( PATH_OUTDIR ) ) 
                    {
                        charParserState = CharParserState.PARSE_OUTDIR;
                    }
                }
            
                break;

            case PARSE_CONFIGPLATFORM_GROUP:
                
                //Here we use the same strategy and make the same assumptions as above
                if ( condition == null
                    || ( condition != null && condition.contains( getConfigurationPlatform() ) ) )
                {
                    if ( path.equals( PATH_ADDITIONAL_INCDIRS ) ) 
                    {
                        charParserState = CharParserState.PARSE_INCLUDE_DIRS;
                    }
                    
                    if ( path.equals( PATH_PREPROCESSOR_DEFS ) ) 
                    {
                        charParserState = CharParserState.PARSE_PREPROCESSOR_DEFS;
                    }
                }
                
                break;
            
            default: 
                
                /* We are looking for a <PropertyGroup> element that either matches the required platform/configuration 
                 * pair through a Condition attribute, or that contains child elements that match the required 
                 * platform/configuration pair (using a similar Condition attribute), for example:
                 *    <ProperyGroup Condition="'$(Configuration)|$(Platform)'=='Debug|Win32'"> ... </ProperyGroup>
                 * or
                 *    <ProperyGroup> 
                 *        <OutDir Condition="'$(Configuration)|$(Platform)'=='Debug|Win32'"> ... </OutDir>
                 *        ...
                 *    </ProperyGroup>
                 * 
                 * If there is no Condition attribute in the <ProperyGroup> element, then we assume that the Condition 
                 * attribute will appear in (all) the child elements contained within <ProperyGroup></ProperyGroup>; we 
                 * use the same strategy and assumptions for a <ItemDefinitionGroup> element.
                 */
                if ( condition == null || condition.contains( getConfigurationPlatform() ) )
                {
                    if ( path.equals( PATH_PROPERTY_GROUP ) )
                    {
                        elementParserState = ElementParserState.PARSE_PROPERTY_GROUP;
                    }
                    else if ( path.equals( PATH_ITEM_DEFINITION_GROUP ) )
                    {
                        elementParserState = ElementParserState.PARSE_CONFIGPLATFORM_GROUP;
                    }
                }
            }
        }

        @Override
        public void endElement( String uri, String localName, String qName ) 
                throws SAXException 
        {
            if ( path.equals( PATH_PROPERTY_GROUP ) ) 
            {
                elementParserState = ElementParserState.PARSE_IGNORE;
            }
            if ( path.equals( PATH_ITEM_DEFINITION_GROUP ) ) 
            {
                elementParserState = ElementParserState.PARSE_IGNORE;
            }

            charParserState = CharParserState.PARSE_IGNORE;
            path.remove( path.lastIndexOf( qName ) );
        }
        
        @Override
        public void characters( char[] chars, int start, int length ) 
                throws SAXException 
        {
            if ( charParserState == CharParserState.PARSE_IGNORE )
            {
                return;
            }

            //Keep variable replacement here as splitEntries, later, gets rid of all variables that were not replaced
            String entries = replaceEnvVariables( new String( chars, start, length ) );
            
            switch ( charParserState ) 
            {
            
            //The project specifies an output directory, possibly different from the default
            case PARSE_OUTDIR: 
                outputDirectory = new File( entries );
                
                //If the output directory is not absolute, then it is relative to the project directory. The solution
                // directory does not come into play here (otherwise the output directory would be an absolute path).
                if ( ! outputDirectory.isAbsolute() ) 
                {
                    outputDirectory = new File( getInputFile().getParentFile(), outputDirectory.getPath() );
                }
                
                break;

            //The project specifies some additional header locations
            case PARSE_INCLUDE_DIRS:
                for ( String directory : splitEntries( entries ) )
                {
                    includeDirs.add( new File( directory ) );
                }
                
                break;
                
            //The project specifies some preprocessor definitions
            case PARSE_PREPROCESSOR_DEFS:
                preprocessorDefs = splitEntries( entries );
                break;
                
            default:
                throw new SAXException( "Invalid character parser state" );
                
            }
        }
        
        private String replaceEnvVariables( String entries )
        {
            //(Reluctantly) Match environment variable names in the format: $(variable_name), use a group to retrieve
            // variable_name without surrounding markers
            Matcher envVariableMatcher = Pattern.compile( "\\$\\((.+?)\\)" ).matcher( entries );
            StringBuffer parsedEntires = new StringBuffer();
            
            //Parse "entries" and find variable names in it
            while ( envVariableMatcher.find() )
            {
                //Extract a matched variable name and check whether it is present in the environment variable map
                String envVariableValue = envVariables.get( envVariableMatcher.group( 1 ) );
                
                if ( envVariableValue != null )
                {
                    //Replace the matched variable name with the corresponding value  
                    envVariableMatcher.appendReplacement( parsedEntires, Matcher.quoteReplacement( envVariableValue ) );
                }
            }

            //Append the rest of "entries" that was not matched in the loop above to the final result
            envVariableMatcher.appendTail( parsedEntires );
            
            return parsedEntires.toString();
        }
        
        private List<String> splitEntries( String entries ) 
        {
            List<String> entryList = new ArrayList<String>();
            
            for ( String entry : entries.split( ";" ) ) 
            {
                if ( !entry.startsWith( "%" ) && !entry.startsWith( "$" ) && !entry.trim().isEmpty() ) 
                {
                    entryList.add( entry.trim() );
                }
            }
            
            return entryList;
        }
    }
    
    /**
     * Retrieve the default output directory for the Visual C++ project.
     * @return the default output directory for the Visual C++ project
     */
    private File getDefaultOutputDirectory()
    {
        //The default output directory is the configuration name
        String childOutputDirectory = getConfiguration();
        
        //However, for platforms others than Win32, the default output directory becomes platform/configuration
        if ( ! getPlatform().equals( "Win32" ) )
        {
            childOutputDirectory = new File( getPlatform(), childOutputDirectory ).getPath();
        }
        
        //Place the default output directory within the appropriate base directory
        return new File( getBaseDirectory(), childOutputDirectory );
    }
    
    /**
     * Retrieve the base directory for the Visual C++ project. If this project is part of a Visual Studio solution, the
     * base directory is the solution directory; for standalone projects, the base directory is the project directory.
     * @return the base directory for the Visual C++ project
     */
    private File getBaseDirectory()
    {
        File referenceFile = ( solutionFile != null ? solutionFile : getInputFile() );
        return referenceFile.getParentFile().getAbsoluteFile();
    };    
    
    private enum ElementParserState 
    {
        PARSE_IGNORE,
        PARSE_PROPERTY_GROUP,
        PARSE_CONFIGPLATFORM_GROUP,
    }

    private enum CharParserState 
    {
        PARSE_IGNORE,
        PARSE_OUTDIR,
        PARSE_INCLUDE_DIRS,
        PARSE_PREPROCESSOR_DEFS
    }    
    
    private SAXParser parser = null; 
    private List<String> path = new ArrayList<String>(); 
    private ElementParserState elementParserState = ElementParserState.PARSE_IGNORE;
    private CharParserState charParserState = CharParserState.PARSE_IGNORE;
    private List<File> includeDirs = new ArrayList<File>();
    private List<String> preprocessorDefs = new ArrayList<String>();
    private File outputDirectory;
    private File solutionFile;
    private Map<String, String> envVariables = new HashMap<String, String>( System.getenv() );
}

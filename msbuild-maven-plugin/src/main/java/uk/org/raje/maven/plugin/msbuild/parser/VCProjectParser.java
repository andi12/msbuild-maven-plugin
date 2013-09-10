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
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;



/**
 * @author dmasato
 *
 */
public class VCProjectParser extends BaseParser 
{
    private static final String PATH_SEPARATOR = "/";
    private static final String PATH_ROOT = "ROOT";
    private static final String PATH_ITEMDEF_GROUP = PATH_ROOT + PATH_SEPARATOR + "Project" + PATH_SEPARATOR
            + "ItemDefinitionGroup";
    
    private static final String PATH_CLCOMPILE = PATH_ITEMDEF_GROUP + PATH_SEPARATOR + "ClCompile";
    private static final String PATH_ADDITIONAL_INCDIRS = PATH_CLCOMPILE + PATH_SEPARATOR 
            + "AdditionalIncludeDirectories";
    
    private static final String PATH_PREPROCESSOR_DEFS = PATH_CLCOMPILE + PATH_SEPARATOR + "PreprocessorDefinitions";
    
    public VCProjectParser( File projectFile, String configuration, String platform ) 
            throws FileNotFoundException, SAXException, ParserConfigurationException 
        {
        
        super( projectFile, configuration, platform );
        SAXParserFactory factory = SAXParserFactory.newInstance();

        parser = factory.newSAXParser();
        elementParserState = ElementParserState.PARSE_IGNORE;
        charParserState = CharParserState.PARSE_IGNORE;
        paths = new LinkedList<String>( Arrays.asList( PATH_ROOT ) );
        includeDirs = new ArrayList<String>();
        preprocessorDefs = new ArrayList<String>();
    }

    public void updateProject( VCProject project )
    {
        if ( project == null ) 
        {
            throw new InvalidParameterException();
        }
        
        project.setPreprocessorDefs( preprocessorDefs );
        project.setIncludeDirs( includeDirs );
    }
    
    public List<String> getIncludeDirs() 
    {
        return includeDirs;
    }

    public List<String> getPreprocessorDefs() 
    {
        return preprocessorDefs;
    }
    
    @Override
    public void parse() throws IOException, ParseException 
    {
        DefaultHandler handler = new DefaultHandler() {
            @Override
            public void startElement( String uri, String localName, String qName, Attributes attributes ) 
                    throws SAXException 
                {
                
                String path = paths.peek() + PATH_SEPARATOR + qName;
                paths.push( path );
                
                switch ( elementParserState ) 
                {
                case PARSE_CONFIGPLATFORM_GROUP: 
                    if ( path.compareTo( PATH_ADDITIONAL_INCDIRS ) == 0 ) 
                    {
                        charParserState = CharParserState.PARSE_INCLUDE_DIRS;
                    }
                    
                    if ( path.compareTo( PATH_PREPROCESSOR_DEFS ) == 0 ) 
                    {
                        charParserState = CharParserState.PARSE_PREPROCESSOR_DEFS;
                    }
                    
                    break;
                
                default: 
                    if ( path.compareTo( PATH_ITEMDEF_GROUP ) == 0 
                        && attributes.getValue( "Condition" ).contains( getRequiredConfigurationPlatform() ) ) 
                    {
                        
                        elementParserState = ElementParserState.PARSE_CONFIGPLATFORM_GROUP;
                    }
                }
            }

            @Override
            public void endElement( String uri, String localName, String qName ) 
                    throws SAXException 
                    {
                
                paths.pop();
                charParserState = CharParserState.PARSE_IGNORE;

                if ( qName == PATH_ITEMDEF_GROUP ) 
                {
                    elementParserState = ElementParserState.PARSE_IGNORE;
                }
            }
            
            @Override
            public void characters( char[] chars, int start, int length ) 
                    throws SAXException 
                    {

                String entries = new String( chars, start, length );
                
                switch ( charParserState ) 
                {
                case PARSE_INCLUDE_DIRS:
                    entries.replace( "$(Configuration)", getRequiredConfiguration() );
                    entries.replace( "$(Platform)", getRequiredPlatform() );
                    includeDirs = splitEntries( entries );
                    break;
                    
                case PARSE_PREPROCESSOR_DEFS:
                    preprocessorDefs = splitEntries( entries );
                    break;
                    
                default:
                }
            }
            
            private List<String> splitEntries( String entries ) 
            {
                List<String> entryList = new ArrayList<String>();
                
                for ( String entry : entries.split( ";" ) ) 
                {
                    if ( !entry.startsWith( "%" ) && !entry.startsWith( "$" ) && !entry.trim().isEmpty() ) 
                    {                        
                        entryList.add( entry );
                    }
                }
                
                return entryList;
            }
        };
        
        try 
        {
            parser.parse( getInputFile(), handler );
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
    
    private enum ElementParserState 
    {
        PARSE_IGNORE,
        PARSE_CONFIGPLATFORM_GROUP,
    }

    private enum CharParserState 
    {
        PARSE_IGNORE,
        PARSE_INCLUDE_DIRS,
        PARSE_PREPROCESSOR_DEFS
    }
    
    private SAXParser parser = null; 
    private LinkedList<String> paths = null; 
    private ElementParserState elementParserState = null;
    private CharParserState charParserState = null;
    private List<String> includeDirs = null;
    private List<String> preprocessorDefs = null;
}

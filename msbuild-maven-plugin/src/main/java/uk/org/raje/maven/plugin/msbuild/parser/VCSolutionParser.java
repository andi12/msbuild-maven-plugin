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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * @author dmasato
 *
 */
public class VCSolutionParser extends BaseParser 
{
    private enum ProjectProperty 
    {
        solutionGuid 
        { 
            @Override
            String getRegex() 
            {
                return "Project\\(" + getGUIDPattern( name() ) + "\\)=";
            } 
        },
        name 
        {
            @Override
            String getRegex() 
            {
                return getStringPattern( name() ) + ",";
            } 
        },
        path 
        {
            @Override
            String getRegex() 
            {
                return getPathPattern( name() ) + ",";
            } 
        },
        guid 
        {
            @Override
            String getRegex() 
            {
                return getGUIDPattern( name() );
            };
        };
        
        String getValue( Matcher matcher ) 
        {
            return matcher.group( name() );
        }

        abstract String getRegex();
    }
    
    private static final String SLN_BEGIN_GLOBAL_SECTION = "GlobalSection(SolutionConfigurationPlatforms)";
    private static final String PRJ_BEGIN_GLOBAL_SECTION = "GlobalSection(ProjectConfigurationPlatforms)";
    private static final String SLN_END_GLOBAL_SECTION = "EndGlobalSection";
    private static final String PRJ_END_GLOBAL_SECTION = "EndGlobalSection";
    
    public VCSolutionParser( File solutionFile, String configuration, String platform, String excludeProjectRegex ) 
            throws FileNotFoundException 
        {
        
        super( solutionFile, configuration, platform );
        StringBuffer projectPatternRegex = new StringBuffer(); 
        
        solutionParserState = SolutionParserState.PARSE_IGNORE;
        configPlatformFound = false;
        projects = new HashMap<String, VCProject>();

        for ( ProjectProperty property : ProjectProperty.values() ) 
        {
            projectPatternRegex.append( property.getRegex() );
        }
        
        projectPattern = Pattern.compile( projectPatternRegex.toString() );
        projectExcludePattern = Pattern.compile( excludeProjectRegex == null ? "" : excludeProjectRegex );
    }
    
    public Collection<VCProject> getProjects() 
    {
        return projects.values();
    }
    
    @Override
    public void parse() throws IOException, ParseException 
    {
        String line;
        BufferedReader reader = new BufferedReader( new FileReader( getInputFile() ) );
        
        while ( ( line = reader.readLine() ) != null ) 
        {
            line = line.replaceAll( "[ \t]", "" );
            
            switch ( solutionParserState ) 
            {
            case PARSE_SOLUTION_CONFIG:
                if ( line.startsWith( SLN_END_GLOBAL_SECTION ) ) 
                {
                    solutionParserState = SolutionParserState.PARSE_IGNORE;
                }
                else 
                {
                    parseSolutionConfig( line );
                }
                
                break;
                
            case PARSE_PROJECT_CONFIG:
                if ( line.startsWith( PRJ_END_GLOBAL_SECTION ) ) 
                {
                    solutionParserState = SolutionParserState.PARSE_IGNORE;
                }
                else 
                {
                    parseProjectConfig( line );
                }
                
                break;
                
            default:
                Matcher prjMatcher = projectPattern.matcher( line );
                Matcher prjExcludeMatcher = projectExcludePattern.matcher( line );
                
                if ( prjMatcher.matches() && !prjExcludeMatcher.matches() ) 
                {
                    parseProjectEntry( prjMatcher );
                }
                else if ( line.startsWith( SLN_BEGIN_GLOBAL_SECTION ) ) 
                {
                    solutionParserState = SolutionParserState.PARSE_SOLUTION_CONFIG;
                }
                else if ( line.startsWith( PRJ_BEGIN_GLOBAL_SECTION ) ) 
                {
                    solutionParserState = SolutionParserState.PARSE_PROJECT_CONFIG;
                }
            }
        }
        
        reader.close();
        validateProjectConfigs();
    }
    
    private void parseSolutionConfig( String line ) 
    {
        final int slnConfigPlatformId = 1;

        String slnConfigPlatform = line.split( "=" )[slnConfigPlatformId];
        
        if ( slnConfigPlatform.compareTo( getRequiredConfigurationPlatform() ) == 0 ) 
        {
            configPlatformFound = true;
            //print("Found solution configuration: {:s}".format(solnConfigPlatform))
        }
    }
    
    private void parseProjectConfig( String line ) 
    {
        final int slnProjectGuidId = 0;
        final int slnConfigPlatformId = 1;
        final int slnProjectConfigId = 2;
        final int prjConfigPlatformId = 1;
        final int prjConfigEntryId = 0;
        final int prjPlatformEntryId = 1;
        
        String slnProjectEntry[] = line.split( "\\." );
        String prjGUID = slnProjectEntry[slnProjectGuidId];
        String slnConfigPlatform = slnProjectEntry[slnConfigPlatformId];
        String prjConfigEntry = slnProjectEntry[slnProjectConfigId]; 
            
        if ( projects.containsKey( prjGUID ) && slnConfigPlatform.compareTo( getRequiredConfigurationPlatform() ) == 0 
                && prjConfigEntry.startsWith( "ActiveCfg" ) ) 
        {

            VCProject project = projects.get( prjGUID );
            String projConfigPlatform[] = prjConfigEntry.split( "=" )[prjConfigPlatformId].split( "\\|" );            
            project.setConfiguration( projConfigPlatform[prjConfigEntryId] );
            project.setPlatform( projConfigPlatform[prjPlatformEntryId] );
        
            /*print("Found project configuration: {:s} {:s} ({:s})".
                  format(project["name"], projConfigPlatform, solnConfigPlatform))*/
        }
    }
    
    private void parseProjectEntry( Matcher projMatcher ) 
    {
        VCProject project = new VCProject( ProjectProperty.name.getValue( projMatcher ), 
                new File( ProjectProperty.path.getValue( projMatcher ) ) );
        
        project.setGuid( ProjectProperty.guid.getValue( projMatcher ) );
        project.setSolutionGuid( ProjectProperty.solutionGuid.getValue( projMatcher ) );
            
        projects.put( project.getGuid(), project );
        //print("Found project: {:s}".format(projMatch.group("name")))
    }
    
    private void validateProjectConfigs() throws ParseException 
    {
        if ( !configPlatformFound ) 
        {
            throw new ParseException( "Required configuration|platform " + getRequiredConfigurationPlatform() 
                    + " was not found in the solution", 0 );
        }
            
        for ( VCProject project: projects.values() ) 
        {
            if ( project.getConfiguration() == null ) 
            {
                throw new ParseException( "Required configuration|platform " + getRequiredConfigurationPlatform() 
                        + " was not found in project " + project.getName(), 0 );
            }
        }
    }
        
    private static String getGUIDPattern( String groupName ) 
    {
        return "\"(?<" + groupName + ">\\{[\\w-]+\\})\"";
    }

    private static String getStringPattern( String groupName ) 
    {
        return "\"(?<" + groupName + ">\\w+)\"";
    }
    
    private static String getPathPattern( String groupName ) 
    {
        return "\"(?<" + groupName + ">.+\\.vcxproj)\"";
    }
    
    private enum SolutionParserState 
    {
        PARSE_IGNORE,
        PARSE_SOLUTION_CONFIG,
        PARSE_PROJECT_CONFIG
    }
    
    private SolutionParserState solutionParserState;
    private boolean configPlatformFound;
    private Pattern projectPattern;
    private Pattern projectExcludePattern;
    private Map<String, VCProject> projects;
}

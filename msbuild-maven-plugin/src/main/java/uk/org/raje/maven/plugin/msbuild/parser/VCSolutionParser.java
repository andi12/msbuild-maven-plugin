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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Class that parses a Visual Studio solution file containing Visual C++ projects. This class identifies all projects
 * entries contained in a given solution according to a specific platform and configuration combination. It then creates
 * corresponding {@link VCProject} beans and populate them with properties from the solution file. Further parsing of  
 * the Visual C++ projects through {@link VCProjectParser} is still required to fully populate the properties in the  
 * generated beans.<p>
 * <p>The solution file contains a list of supported platform/configuration pairs (<em>e.g</em>. 
 * {@code Win32/Release}); for each pair, the solution also specify a given platform/configuration pair for each
 * project entry in the solution. Note that Visual Studio allows the platform/configuration pair for a projects to be 
 * different from the solution platform/configuration pair.</p>
 */
class VCSolutionParser extends BaseParser 
{
    /**
     * Create an instance of the Visual Studio solution parser.
     * @param solutionFile the solution file ({@code .sln}) to analyse
     * @param platform the platform for which to retrieve Visual C++ projects (<em>e.g</em>. {@code Win32}, 
     * {@code x64})
     * @param configuration the configuration for which to retrieve Visual C++ projects (<em>e.g.</em> 
     * {@code Release}, {@code Debug})
     * @throws FileNotFoundException if the given solution file is not found
     */
    public VCSolutionParser( File solutionFile, String platform, String configuration ) 
            throws FileNotFoundException 
    {
        super( solutionFile, platform, configuration );
        StringBuffer projectPatternRegex = new StringBuffer(); 
        
        solutionParserState = SolutionParserState.PARSE_IGNORE;
        isSolutionConfigPlatformSupported = false;
        projects = new HashMap<String, VCProject>();

        /*
         * Build a regex to parse a Visual C++ project line in the solution file. A project line looks like this:
         * 
         * Project("{8BC9CEB8-8B4A-11D0-8D11-00A0C91BC942}") = "hello-world", "hello-world-project\hello-world.vcxproj",
         * "{5AF88374-A467-4CCA-8B38-CEB0DDE9BA58}"
         * 
         * where the 4 strings represent: the solution GUID, the project name, the project path relative to the solution
         * path, and finally the project GUID.
         */
        for ( ProjectProperty property : ProjectProperty.values() ) 
        {
            projectPatternRegex.append( property.getRegex() );
        }
        
        projectPropertiesPattern = Pattern.compile( projectPatternRegex.toString() );
    }
    
    /**
     * get the list of Visual C++ projects contained in the given solution.
     * @return the list of Visual C++ projects contained in the solution
     */
    public List<VCProject> getVCProjects() 
    {
        return new ArrayList<VCProject>( projects.values() );
    }
    
    @Override
    public void parse() throws IOException, ParseException 
    {
        String line;
        BufferedReader reader = new BufferedReader( new FileReader( getInputFile() ) );
        
        while ( ( line = reader.readLine() ) != null ) 
        {
            //Remove all whitespace, it makes the lines easier to analyse via regexs.
            line = line.replaceAll( "[ \t]", "" );
            
            switch ( solutionParserState ) 
            {
            //Parse the solution global section, which contains the list of supported platform/configuration pairs for 
            // the solution.
            case PARSE_SOLUTION_GLOBAL_SECTION:
                if ( line.startsWith( END_SOLUTION_GLOBAL_SECTION ) ) 
                {
                    solutionParserState = SolutionParserState.PARSE_IGNORE;
                }
                else 
                {
                    parseSolutionConfig( line );
                }
                
                break;
                
            //Parse the project global section, which contains the list of supported platform/configuration pairs for 
            // each project, for each platform/configuration pair supported by the solution (e.g. a solution that
            // supports the Win32/Release pair may specify that a project has to be built against a Win32/Debug pair). 
            case PARSE_PROJECT_GLOBAL_SECTION:
                if ( line.startsWith( END_PROJECT_GLOBAL_SECTION ) ) 
                {
                    solutionParserState = SolutionParserState.PARSE_IGNORE;
                }
                else 
                {
                    parseProjectPlatformConfig( line );
                }
                
                break;
            
            //Parse the rest of the solution file.
            default:
                Matcher prjMatcher = projectPropertiesPattern.matcher( line );
                
                if ( prjMatcher.matches() ) 
                {
                    parseProjectEntry( prjMatcher );
                }
                else if ( line.startsWith( BEGIN_SOLUTION_GLOBAL_SECTION ) ) 
                {
                    solutionParserState = SolutionParserState.PARSE_SOLUTION_GLOBAL_SECTION;
                }
                else if ( line.startsWith( BEGIN_PROJECT_GLOBAL_SECTION ) ) 
                {
                    solutionParserState = SolutionParserState.PARSE_PROJECT_GLOBAL_SECTION;
                }
            }
        }
        
        reader.close();
        validateProjectPlatformConfigs();
    }
    
    private static final String BEGIN_SOLUTION_GLOBAL_SECTION = "GlobalSection(SolutionConfigurationPlatforms)";
    private static final String BEGIN_PROJECT_GLOBAL_SECTION = "GlobalSection(ProjectConfigurationPlatforms)";
    private static final String END_SOLUTION_GLOBAL_SECTION = "EndGlobalSection";
    private static final String END_PROJECT_GLOBAL_SECTION = "EndGlobalSection";

    private static String getGUIDPattern( String groupName ) 
    {
        return "\"(?<" + groupName + ">\\{[\\w-]+\\})\"";
    }

    private static String getStringPattern( String groupName ) 
    {
        return "\"(?<" + groupName + ">[\\w-]+)\"";
    }
    
    private static String getPathPattern( String groupName ) 
    {
        return "\"(?<" + groupName + ">.+\\.vcxproj)\"";
    }
    
    private enum SolutionParserState 
    {
        PARSE_IGNORE,
        PARSE_SOLUTION_GLOBAL_SECTION,
        PARSE_PROJECT_GLOBAL_SECTION
    }

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
        relativePath 
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
    
    private void parseSolutionConfig( String line ) 
    {
        final int slnConfigPlatformId = 1;

        //Retrieve one of the supported platform/configuration pairs for the solution; also check whether this pair
        // matches the one we are looking for.
        String slnConfigPlatform = line.split( "=" )[slnConfigPlatformId];
        
        if ( slnConfigPlatform.compareTo( getConfigurationPlatform() ) == 0 ) 
        {
            isSolutionConfigPlatformSupported = true;
        }
    }
    
    private void parseProjectEntry( Matcher projMatcher ) 
    {
        //Compute the full project path by joining the solution path with the relative project path.
        String relativeProjectPath = ProjectProperty.relativePath.getValue( projMatcher );
        File fullProjectPath = new File( getInputFile().getParentFile(), relativeProjectPath );
        
        //Create and populate a new bean for this project.
        VCProject project = new VCProject( ProjectProperty.name.getValue( projMatcher ), fullProjectPath );
        project.setTargetName( new File( relativeProjectPath ).getParent() );
        project.setGuid( ProjectProperty.guid.getValue( projMatcher ) );
        project.setSolutionGuid( ProjectProperty.solutionGuid.getValue( projMatcher ) );
        
        projects.put( project.getGuid(), project );
    }
    
    private void parseProjectPlatformConfig( String line ) 
    {
        final int solutionProjectGuidId = 0;
        final int solutionConfigurationPlatformId = 1;
        final int solutionProjectConfigurationId = 2;
        final int projectConfigurationPlatformId = 1;
        final int projectConfigurationEntryId = 0;
        final int projectPlatformEntryId = 1;
        
        /*
         * A supported platform/configuration pair for a project looks like this:
         * 
         * {5AF88374-A467-4CCA-8B38-CEB0DDE9BA58}.Debug|Win32.ActiveCfg = Debug|Win32
         * (                 A                  ).(    B    ).(   C   ) = (    D    )
         * 
         * A = project GUID
         * B = solution platform/configuration pair
         * C = this is an active project platform/configuration pair
         * D = project platform/configuration pair for this solution platform/configuration pair
         */
        String solutionProjectEntries[] = line.split( "\\." );
        String projectGUID = solutionProjectEntries[solutionProjectGuidId];
        String solutionConfigurationPlatform = solutionProjectEntries[solutionConfigurationPlatformId];
        String projectActiveConfigEntry = solutionProjectEntries[solutionProjectConfigurationId]; 

        //If the project GUID is in the list of projects for this solution, and the solution platform/configuration pair
        // matches the one we are looking for, it means we found a platform/configuration pair for this project.
        if ( projects.containsKey( projectGUID ) 
                && projectActiveConfigEntry.startsWith( "ActiveCfg" )  
                && solutionConfigurationPlatform.compareTo( getConfigurationPlatform() ) == 0 ) 
        {
            VCProject project = projects.get( projectGUID );
            String projConfigurationPlatform[] = 
                    projectActiveConfigEntry.split( "=" )[projectConfigurationPlatformId].split( "\\|" );
            
            project.setConfiguration( projConfigurationPlatform[projectConfigurationEntryId] );
            project.setPlatform( projConfigurationPlatform[projectPlatformEntryId] );
        }
    }
    
    private void validateProjectPlatformConfigs() throws ParseException 
    {
        if ( ! isSolutionConfigPlatformSupported ) 
        {
            throw new ParseException( "Required configuration|platform " + getConfigurationPlatform() 
                    + " was not found in the solution", 0 );
        }
            
        for ( VCProject project: projects.values() ) 
        {
            if ( project.getConfiguration() == null ) 
            {
                throw new ParseException( "Required configuration|platform " + getConfigurationPlatform() 
                        + " was not found in project " + project.getName(), 0 );
            }
        }
    }
        
    private SolutionParserState solutionParserState;
    private boolean isSolutionConfigPlatformSupported;
    private Pattern projectPropertiesPattern;
    private Map<String, VCProject> projects;
}

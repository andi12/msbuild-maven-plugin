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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;

/**
 * Collection of utilities used by Mojo's in the msbuild-maven-plugin.
 */
public class MojoHelper 
{
    // no instances
    private MojoHelper()
    {
    }
    
    /**
     * Validates the path to a command-line tool.
     * @param toolPath the configured tool path from the POM
     * @param toolName the name of the tool, used for logging messages
     * @param logger a Log to write messages to
     * @throws FileNotFoundException if the tool cannot be found
     */
    public static void validateToolPath( File toolPath, String toolName, Log logger ) 
            throws FileNotFoundException
    {
        logger.debug( "Validating path for " + toolName );
        
        if ( toolPath == null )
        {
            logger.error( "Missing " + toolName + " path" );
            throw new FileNotFoundException();
        }
        
        if ( !toolPath.exists() || !toolPath.isFile() )
        {
            logger.error( "Could not find " + toolName + " at " + toolPath );
            throw new FileNotFoundException( toolPath.getAbsolutePath() );
        }
        
        logger.debug( "Found " + toolName + " at " + toolPath );
    }    
        
    /**
     * Check that we have a valid set of platforms.
     * If no platforms are configured we create 1 default platform.
     * @param platforms the list of BuildPlatform's to validate
     * @return the passed in list or a new list with a default platform added
     * @throws MojoExecutionException if the configuration is invalid.
     */
    public static List<BuildPlatform> validatePlatforms( List<BuildPlatform> platforms ) throws MojoExecutionException
    {
        if ( platforms == null )
        {
            platforms = new ArrayList<BuildPlatform>();
            platforms.add( new BuildPlatform() );
        }
        else
        {
            Set<String> platformNames = new HashSet<String>();
            for ( BuildPlatform platform : platforms )
            {
                if ( platformNames.contains( platform.getName() ) )
                {
                    throw new MojoExecutionException( "Duplicate platform '" + platform.getName()
                            + "' in configuration, platform names must be unique" );
                }
                platformNames.add( platform.getName() );
                platform.identifyPrimaryConfiguration();
            }
        }
        return platforms;
    }

}

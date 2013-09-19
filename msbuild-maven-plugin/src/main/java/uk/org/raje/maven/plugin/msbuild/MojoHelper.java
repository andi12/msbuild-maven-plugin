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

import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
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
     * @param toolPathEnvVar the name of an environment variable to look up the path in if toolPath is null
     * @param toolName the name of the tool, used for logging messages
     * @param logger a Log to write messages to
     * @throws FileNotFoundException if the tool cannot be found
     */
    public static void validateToolPath( File toolPath, String toolPathEnvVar, String toolName, Log logger ) 
            throws FileNotFoundException
    {
        logger.debug( "Validating path for " + toolName + "." );
        
        if ( toolPath == null )
        {
            // not set in configuration try system environment
            String toolEnvPath = System.getenv( toolPathEnvVar );
            
            if ( toolEnvPath != null )
            {
                toolPath = new File( toolEnvPath );
            }
        }
        
        if ( toolPath == null )
        {
            logger.error( "Missing " + toolName + " path. " );
            throw new FileNotFoundException();
        }
        
        if ( !toolPath.exists() || !toolPath.isFile() )
        {
            logger.error( "Could not find " + toolName + " at " + toolPath + "." );
            throw new FileNotFoundException( toolPath.getAbsolutePath() );
        }
        
        logger.debug( "Found " + toolName + " at " + toolPath + "." );
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

    /**
     * Calculate the directory that msbuild will write output files to for a given platform and configuration
     * @param projectDirectory the directory containing the project/solution file
     * @param p the BuildPlatform
     * @param c the BuildConfiguration
     * @param log a Log to write to
     * @return a File object for the output directory
     * @throws MojoExecutionException if an output directory cannot be determined
     */
    public static File getConfigurationOutputDirectory( File projectDirectory, 
            BuildPlatform p, BuildConfiguration c, Log log ) throws MojoExecutionException
    {
        File result = null;
        
        // If there is a configured value use it
        result = c.getOutputDirectory();
        if ( result == null )
        {
            // There isn't a configured value so work it out
            if ( p.isWin32() )
            {
                // A default Win32 project writes Win32 outputs at the top level
                result = new File( projectDirectory, c.getName() );
                if ( result.exists() )
                {
                    log.debug( "Found output directory for Win32 at " + result.getAbsolutePath() );
                }
                else
                {
                    // Nothing there, fall through and try platform\configuration
                    result = null;
                }
            }

            if ( result == null )
            {
                // Assume that msbuild has created an output folder named
                // after the platform and configuration
                result = new File( projectDirectory, p.getName() );
                result = new File ( result, c.getName() );
            }
        }

        // result will be populated, now check if it was created
        if ( result.exists() && result.isDirectory() )
        {
            return result;
        }
        else
        {
            String exceptionMessage = "Expected output directory was not created, configuration error?"; 
            log.error( exceptionMessage );
            log.error( "Looking for build output at " + result.getAbsolutePath() );
            throw new MojoExecutionException( exceptionMessage );
        }
    }
}

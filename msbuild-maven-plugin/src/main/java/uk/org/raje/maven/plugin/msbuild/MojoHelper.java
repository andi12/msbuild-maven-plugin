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
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.cli.StreamConsumer;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;

/**
 * @author dmasato
 *
 */
public class MojoHelper 
{
    // no instances
    private MojoHelper()
    {
    }
    
    /**
     * Validates the path to a command-line tool.
     * @throws FileNotFoundException if the tool cannot be found
     */
    public static void validateToolPath( File toolPath, String toolPathEnvVar, String toolName, Log logger ) 
            throws FileNotFoundException
    {
        if ( toolPath == null )
        {
            // not set in configuration try system environment
            String toolEnvPath = System.getenv( toolPathEnvVar );
            
            if ( toolEnvPath != null )
            {
                toolPath = new File( toolEnvPath );
            }
        }
        
        if ( toolPath == null || !toolPath.exists() || !toolPath.isFile() )
        {
            throw new FileNotFoundException( toolPath == null ? "Missing " + toolName + " path " : toolPath.getName() );
        }
        
        logger.debug( toolName + " path: " + toolPath );
    }    
        
    /**
     * Check that we have a valid project or solution file.
     * @param packaging the project packing
     * @param projectFile the configured project file to check
     * @param log a Log to write to
     * @throws MojoExecutionException if the specified projectFile is invalid.
     */
    protected static void validateProjectFile( String packaging, File projectFile, Log log ) 
            throws MojoExecutionException
    {
        if ( projectFile != null
                && projectFile.exists()
                && projectFile.isFile() )
        {
            log.debug( "Project file validated at " + projectFile );

            boolean solutionFile = projectFile.getName().toLowerCase().endsWith( "." + SOLUTION_EXTENSION ); 
            if ( ( MSBuildPackaging.isSolution( packaging ) && ! solutionFile )
                    || ( ! MSBuildPackaging.isSolution( packaging ) && solutionFile ) )
            {
                // Solution packaging defined but the projectFile is not a .sln
                String msg = "You must specify a solution file when packaging is " + MSBuildPackaging.MSBUILD_SOLUTION;
                log.error( msg );
                throw new MojoExecutionException( msg );
            }
            return;
        }
        String prefix = "Missing projectFile";
        if ( projectFile != null )
        {
            prefix = ". The specified projectFile '" + projectFile
                    + "' is not valid";
        }
        throw new MojoExecutionException( prefix
                + ", please check your configuration" );
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

    /**
     * StreamConsumer that writes lines from the stream to the supplied Log at 'info' level.
     */
    protected static class LogOutputStreamConsumer implements StreamConsumer
    {
        public LogOutputStreamConsumer( Log logger ) 
        {
            this.logger = logger;
        }
        
        @Override
        public void consumeLine( String line )
        {
            logger.info( line );
        }
        
        private Log logger;
    }

    /**
     * StreamConsumer that writes lines from the stream to the supplied Log at 'error' level.
     */
    protected static class ErrStreamConsumer implements StreamConsumer
    {
        public ErrStreamConsumer( Log logger ) 
        {
            this.logger = logger;
        }
        
        @Override
        public void consumeLine( String line )
        {
            logger.error( line );
        }
        
        private Log logger;
    }
    
    /**
     * @author dmasato
     *
     */
    protected static class WriterStreamConsumer implements StreamConsumer
    {
        public WriterStreamConsumer( Writer writer ) 
        {
            this.writer = writer;
        }
        
        @Override
        public void consumeLine( String line )
        {
            try 
            {
                writer.write( line );
            } 
            catch ( IOException ioe ) 
            {
                ioe.printStackTrace();
            }
        }
        
        private Writer writer;
    }
    
    /**
     * The file extension for solution files.
     */
    private static final String SOLUTION_EXTENSION = "sln";

}

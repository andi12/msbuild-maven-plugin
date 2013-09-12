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
     * @param log a Log to write to
     * @param packaging the project packing
     * @param projectFile the configured project file to check
     * @throws MojoExecutionException if the specified projectFile is invalid.
     */
    protected static void validateProjectFile( Log log, String packaging, File projectFile ) 
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
     * @author dmasato
     *
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
     * @author dmasato
     *
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

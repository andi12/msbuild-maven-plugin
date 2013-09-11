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
    /**
     * The file extension for solution files.
     */
    public static final String SOLUTION_EXTENSION = "sln";
    
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
     * Is the configured project a solution
     * @return true if the project file name configured ends '.sln'
     */
    public static boolean isSolution( File projectFile )
    {
        boolean result = false;
        
        if ( ( projectFile != null ) 
                && ( projectFile.getName().toLowerCase().endsWith( "." + SOLUTION_EXTENSION ) ) )
        {
            result = true;
        }
        return result;
    }    
    
    /**
     * Check that we have a valid project or solution file.
     * @throws MojoExecutionException if the specified projectFile is invalid.
     */
    public static void validateProjectFile( File projectFile ) throws MojoExecutionException
    {
        if ( projectFile != null
                && projectFile.exists()
                && projectFile.isFile() )
        {
            return;
        }
        String prefix = "Missing projectFile";
        if ( projectFile != null )
        {
            prefix = "The specified projectFile '" + projectFile
                    + "' is not valid";
        }
        throw new MojoExecutionException( prefix
                + ", please check your configuration" );
    }

    /**
     * Check that we have a valid set of platforms.
     * If no platforms are configured we create 1 default platform.
     * @throws MojoExecutionException if the configuration is invalid.
     */
    public static void validatePlatforms( List<BuildPlatform> platforms ) throws MojoExecutionException
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
    
}

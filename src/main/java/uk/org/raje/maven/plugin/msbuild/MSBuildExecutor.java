/*
 * Copyright 2013 Andrew Everitt, Andrew Heckford, Daniele Daniele
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.StreamPumper;

final class MSBuildExecutor
{

    public MSBuildExecutor( Log log, File msbuild, File projectFile )
    {
        this.log = log;
        this.msbuild = msbuild;
        this.projectFile = projectFile;
    }

    /**
     * Provide a set of platforms to build.
     * @param platforms reference to the array of platform strings
     */
    public void setPlatforms( String[] platforms )
    {
        this.platforms = platforms;
    }

    public void setConfiguration( String[] configurations )
    {
        this.configurations = configurations;
    }

    public void execute() throws IOException, InterruptedException
    {
        // TODO: Handle no platforms
        for ( String platform: platforms ) 
        {
            // TODO: Handle no configurations
            for ( String configuration : configurations ) 
            {
                List<String> command = new ArrayList<String>();
                command.add( msbuild.getAbsolutePath() );
                command.add( "/maxcpucount" );
                command.add( "/p:Configuration=" + configuration );
                command.add( "/p:Platform=" + platform );
                //.append("/t:${msbuild-build.targets}")
                command.add( projectFile.getAbsolutePath() );

                ProcessBuilder pb = new ProcessBuilder( command );
                if ( log.isInfoEnabled() )
                {
                    StringBuilder cmdLine = new StringBuilder();
                    for ( String arg : command )
                    {
                        cmdLine.append( arg ).append( " " );
                    }
                    log.info( cmdLine.toString() );
                }
                
                Process proc = pb.start();
                final StreamPumper stdoutPumper = new StreamPumper( proc.getInputStream(), new OutStreamConsumer() );
                stdoutPumper.start();
                final StreamPumper stderrPumper = new StreamPumper( proc.getErrorStream(), new ErrStreamConsumer() );
                stderrPumper.start();
                
                int exitCode = proc.waitFor();
                log.info( "MSBuild returned " + exitCode );
            }
        }
        
    }


    class OutStreamConsumer implements StreamConsumer
    {
        @Override
        public void consumeLine( String arg0 )
        {
            log.info( arg0 );
        }
    }

    class ErrStreamConsumer implements StreamConsumer
    {
        @Override
        public void consumeLine( String arg0 )
        {
            log.error( arg0 );
        }
    }

    private Log log;

    private File msbuild;
    private File projectFile;
    private String[] platforms;
    private String[] configurations;
    private String[] targets;
}

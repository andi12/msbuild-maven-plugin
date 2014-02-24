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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.cli.StreamPumper;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;
import uk.org.raje.maven.plugin.msbuild.streamconsumers.StderrStreamToLog;
import uk.org.raje.maven.plugin.msbuild.streamconsumers.StdoutStreamToLog;

final class MSBuildExecutor
{

    public MSBuildExecutor( Log log, File msbuild, int maxCpuCount, String referencePath, File projectFile )
    {
        this.log = log;
        this.msbuild = msbuild;
        this.maxCpuCount = maxCpuCount;
        this.referencePath = referencePath;
        this.projectFile = projectFile;
    }

    public void setTargets( List<String> targets )
    {
        buildTargets = targets;
    }

    /**
     * Provide a set of platforms to build.
     * @param platforms reference to the array of platform strings
     */
    public void setPlatforms( List<BuildPlatform> platforms )
    {
        buildPlatforms = platforms;
    }

    public void setEnvironment( Map<String, String> environment ) 
    {
        this.environment = environment;
    }

    /**
     * Execute the build.
     * The function assumes that at least 1 platform configuration has been provided
     * in a list via {@link #setPlatforms(List)}.
     * @throws IOException if there is a problem executing MSBuild
     * @throws InterruptedException if execution is interrupted
     */
    public int execute() throws IOException, InterruptedException
    {
        for ( BuildPlatform platform: buildPlatforms ) 
        {
            for ( BuildConfiguration configuration : platform.getConfigurations() ) 
            {
                int exitCode = runMSBuild( platform.getName(), configuration.getName() );
                if ( exitCode != 0 )
                {
                    return exitCode;
                }
            }
        }
        return 0;
    }

    private int runMSBuild( String platform, String configuration ) 
            throws IOException, InterruptedException
    {
        List<String> command = new ArrayList<String>();
        command.add( msbuild.getAbsolutePath() );
        if ( maxCpuCount == -1 )
        {
            command.add( "/maxcpucount" );
        }
        else
        {
            command.add( "/maxcpucount:" + maxCpuCount );
        }
        if ( referencePath != null )
        {
            command.add( "/p:ReferencePath=" + referencePath );
        }
        if ( configuration != null )
        {
            command.add( "/p:Configuration=" + configuration );
        }
        if ( platform != null )
        {
            command.add( "/p:Platform=" + platform );
        }
        if ( buildTargets != null )
        {
            StringBuilder targetsString = new StringBuilder();
            for ( String target: buildTargets )
            {
                targetsString.append( target ).append( ";" );
            }
            targetsString.deleteCharAt( targetsString.length() - 1 );
            command.add( "/t:" + targetsString );
        }
        command.add( projectFile.getAbsolutePath() );

        ProcessBuilder pb = new ProcessBuilder( command );
        
        if ( environment != null )
        {
            pb.environment().putAll( environment );
        }
        
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
        final StreamPumper stdoutPumper = new StreamPumper( proc.getInputStream(), new StdoutStreamToLog( log ) );
        stdoutPumper.start();
        final StreamPumper stderrPumper = new StreamPumper( proc.getErrorStream(), new StderrStreamToLog( log ) );
        stderrPumper.start();
        
        int exitCode = proc.waitFor();
        stdoutPumper.waitUntilDone();
        stderrPumper.waitUntilDone();
        if ( exitCode != 0 )
        {
            log.error( "MSBuild returned non-zero exit code (" + exitCode + ")" );
            log.error( "Error building " + platform + "-" + configuration );
        }
        return exitCode;
    }

    private Log log;

    private File msbuild;
    private int maxCpuCount;
    private String referencePath;
    private File projectFile;
    private List<String> buildTargets;
    private List<BuildPlatform> buildPlatforms;
    private Map<String, String> environment;
}

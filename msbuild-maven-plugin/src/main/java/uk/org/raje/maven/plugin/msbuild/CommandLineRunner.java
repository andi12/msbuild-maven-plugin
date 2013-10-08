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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.StreamPumper;


/**
 *
 */
public abstract class CommandLineRunner 
{
    public CommandLineRunner( StreamConsumer outputConsumer, StreamConsumer errorConsumer )
    {
        this.outputConsumer = outputConsumer;
        this.errorConsumer = errorConsumer;
    }
    
    public int runCommandLine() throws IOException, InterruptedException
    {
        Process commandLineProc;
        List<String> commandLineArguments = getCommandLineArguments();
        ProcessBuilder processBuilder = new ProcessBuilder( commandLineArguments );
        processBuilder.directory( workingDirectory );
        processBuilder.environment().putAll( environmentVars );
        
        logger.fine( "Executing command line: " + getCommandLine() );
        logger.fine( "Working directory: " + workingDirectory );
        logger.fine( "Environemnt variables: " + environmentVars );
        commandLineProc = processBuilder.start();
        
        final StreamPumper stdoutPumper = new StreamPumper( commandLineProc.getInputStream(), outputConsumer );
        final StreamPumper stderrPumper = new StreamPumper( commandLineProc.getErrorStream(), errorConsumer );
        stdoutPumper.start();
        stderrPumper.start();

        int exitCode = commandLineProc.waitFor();
        stdoutPumper.waitUntilDone();
        stderrPumper.waitUntilDone();
        logger.fine( "Command line returned exit code " + exitCode );
        
        return exitCode; 
    }    
    
    public void setWorkingDirectory( File workingDirectory ) 
    {
        this.workingDirectory = workingDirectory;
    }

    public void setEnvironmentVars( Map<String, String> environmentVars ) 
    {
        this.environmentVars = environmentVars;
    }

    protected abstract List<String> getCommandLineArguments();

    protected File getWorkingDirectory() 
    {
        return workingDirectory;
    }

    protected Map<String, String> getEnvironmentVars() 
    {
        return environmentVars;
    }

    protected String getCommandLine() 
    {
        StringBuilder commandLine = new StringBuilder();
        
        for ( String arg : getCommandLineArguments() )
        {
            commandLine.append( arg ).append( " " );
        }
        
        return commandLine.toString();
    }
    
    private final Logger logger = Logger.getLogger( getClass().getName() );
    
    private StreamConsumer outputConsumer;
    private StreamConsumer errorConsumer;
    private File workingDirectory = new File( "." );
    private Map<String, String> environmentVars = new HashMap<String, String>();
}

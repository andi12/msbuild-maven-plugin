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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

import uk.org.raje.maven.plugin.msbuild.MojoHelper.LogOutputStreamConsumer;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;
import uk.org.raje.maven.plugin.msbuild.configuration.CppCheckConfiguration;
import uk.org.raje.maven.plugin.msbuild.parser.VCProject;

/**
 * Configure and run Cppcheck static analysis tool.
 */
@Mojo( name = CppCheckMojo.MOJO_NAME, defaultPhase = LifecyclePhase.VERIFY )
public class CppCheckMojo extends AbstractCodeAnalysisMojo 
{
    /**
     * The name this Mojo declares, also represents the goal.
     */
    public static final String MOJO_NAME = "cppcheck";
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException 
    {
        List<VCProject> vcProjects = null;
        
        if ( !isCppCheckEnabled() ) 
        {
            return;
        }
        
        validateCppCheckConfiguration();
        
        getLog().info( "Performing static code analysis using " + CppCheckConfiguration.CPPCHECK_NAME + "." );
        
        for ( BuildPlatform platform : platforms ) 
        {
            for ( BuildConfiguration configuration : platform.getConfigurations() )
            {
                if ( MSBuildPackaging.isSolution( mavenProject.getPackaging() ) ) 
                {
                    vcProjects = processVCSolutionFile( platform, configuration );
                }
                else 
                {
                    vcProjects = processVCProjectFile( platform, configuration );
                }

                for ( VCProject vcProject : vcProjects )
                {
                    runCppCheck( vcProject, createCppCheckReportWriter( vcProject ) );
                }
            }
        }

        getLog().info( "Static code analysis complete." );
    }
    
    private Writer createCppCheckReportWriter( VCProject vcProject ) throws MojoExecutionException
    {
        BufferedWriter cppCheckReportWriter;
        File cppCheckReport = new File( vcProject.getBaseDir(), cppCheck.reportName() + "-" 
                + vcProject.getPlatform() + "-" + vcProject.getConfiguration() + ".xml" );
        
        try 
        {
            cppCheckReportWriter = new BufferedWriter( new FileWriter( cppCheckReport ) );
        } 
        catch ( IOException ioe ) 
        {
            throw new MojoExecutionException( "Could not create " + CppCheckConfiguration.CPPCHECK_NAME + " report " 
                    + cppCheckReport, ioe );
        }

        return cppCheckReportWriter;
    }
    
    private void runCppCheck( VCProject vcProject, Writer cppCheckReportWriter ) throws MojoExecutionException
    {
        CppCheckRunner cppCheckRunner = new CppCheckRunner( cppCheck.cppCheckPath(), vcProject.getBaseDir(), 
                new LogOutputStreamConsumer( getLog() ), new WriterStreamConsumer( cppCheckReportWriter ) );
        
        cppCheckRunner.setCppCheckType( cppCheck.cppCheckType() );
        cppCheckRunner.setIncludeDirectories( vcProject.getIncludeDirectories() );
        cppCheckRunner.setPreprocessorDefs( vcProject.getPreprocessorDefs() );
        
        getLog().info( "Executing code analysis for project " + vcProject.getName() + "." );
        getLog().debug( "Executing command line " + cppCheckRunner.getCommandLine() );
        
        try
        {
            cppCheckRunner.runCommandLine();
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "I/O error while executing command line ", ioe );
        }
        catch ( InterruptedException ie )
        {
            throw new MojoExecutionException( "Process interrupted while executing command line ", ie );
        }
        
        try 
        {
            cppCheckReportWriter.close();
        } 
        catch ( IOException ioe ) 
        { 
            throw new MojoExecutionException( "Could not finalise " + CppCheckConfiguration.CPPCHECK_NAME + " report", 
                    ioe );
        }

        getLog().info( "Static code analysis for project " + vcProject.getName() + " succeeded." );
    }
}

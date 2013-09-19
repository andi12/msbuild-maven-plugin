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
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.cli.StreamConsumer;
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
    public void doExecute() throws MojoExecutionException, MojoFailureException 
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
                    try 
                    {
                        getLog().info( "Running static code analysis for project " + vcProject.getName() + "." );
                        runCppCheck( vcProject );
                    }
                    catch ( MojoExecutionException mee )
                    {
                        getLog().error( mee.getMessage() );
                        throw mee;
                    }
                }
            }
        }

        getLog().info( "Static code analysis complete." );
    }
    
    /**
     * Runs CppCheck against a given Visual C++ project and produces a static code analysis report
     */    
    private Writer createCppCheckReportWriter( VCProject vcProject ) throws MojoExecutionException
    {
        BufferedWriter cppCheckReportWriter;
        File reportFile = getReportFile( vcProject );
        
        try 
        {
            cppCheckReportWriter = new BufferedWriter( new FileWriter( reportFile ) );
        } 
        catch ( IOException ioe ) 
        {
            throw new MojoExecutionException( "Could not create " + CppCheckConfiguration.CPPCHECK_NAME + " report " 
                    + reportFile, ioe );
        }

        return cppCheckReportWriter;
    }
    
    public void finaliseReportWriter( Writer reportWriter, VCProject vcProject ) throws MojoExecutionException
    {
        try 
        {
            reportWriter.close();
        } 
        catch ( IOException ioe ) 
        { 
            throw new MojoExecutionException( "Could not finalise " + CppCheckConfiguration.CPPCHECK_NAME + " report" 
                    + getReportFile( vcProject ), ioe );
        }
    }
    
    private void runCppCheck( VCProject vcProject ) throws MojoExecutionException
    {
        Writer reportWriter = createCppCheckReportWriter( vcProject );
        CppCheckRunner cppCheckRunner = new CppCheckRunner( cppCheck.getCppCheckPath(), vcProject.getBaseDir(), 
                new LogOutputStreamConsumer( getLog() ), new WriterStreamConsumer( reportWriter ) );
        
        cppCheckRunner.setCppCheckType( cppCheck.getCppCheckType() );
        cppCheckRunner.setIncludeDirectories( vcProject.getIncludeDirectories() );
        cppCheckRunner.setPreprocessorDefs( vcProject.getPreprocessorDefs() );
        
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
        
        finaliseReportWriter ( reportWriter, vcProject );
    }
    
    private File getReportFile( VCProject vcProject ) 
    {
        return new File( vcProject.getBaseDir(), cppCheck.getReportName() + "-" 
                + vcProject.getPlatform() + "-" + vcProject.getConfiguration() + ".xml" );
    }
    
    private class CppCheckRunner extends CommandLineRunner
    {
        public CppCheckRunner( File cppCheckPath, File vcProjectPath, StreamConsumer outputConsumer, 
                StreamConsumer errorConsumer )
        {
            super( outputConsumer, errorConsumer );
            this.cppCheckPath = cppCheckPath;
            this.vcProjectPath = vcProjectPath;
        }
        
        public void setCppCheckType( CppCheckType cppCheckType ) 
        {
            this.cppCheckType = cppCheckType;
        }

        public void setIncludeDirectories( List<String> includeDirectories ) 
        {
            this.includeDirectories = includeDirectories;
        }

        public void setExcludeDirectories( List<String> excludeDirectories ) 
        {
            this.excludeDirectories = excludeDirectories;
        }

        public void setPreprocessorDefs( List<String> preprocessorDefs ) 
        {
            this.preprocessorDefs = preprocessorDefs;
        }    
        
        @Override
        protected List<String> getCommandLineArguments() 
        {
            List<String> commandLineArguments = new LinkedList<String>();
            
            commandLineArguments.add( cppCheckPath.getAbsolutePath() );
            commandLineArguments.add( "--quiet" );
            commandLineArguments.add( "--xml" );
            commandLineArguments.add( "--xml-version=" + CPPCHECK_XML_VERSION );
            commandLineArguments.add( "--enable=" + cppCheckType.name() );
            
            for ( String includeDirectory : includeDirectories ) 
            {
                commandLineArguments.add( "-I" );
                commandLineArguments.add( "\"" + includeDirectory + "\"" );
            }

            for ( String excludeDirectory : excludeDirectories ) 
            {
                commandLineArguments.add( "-i" );
                commandLineArguments.add( "\"" + excludeDirectory + "\"" );
            }

            for ( String preprocessorDef : preprocessorDefs ) 
            {
                commandLineArguments.add( "-D" + preprocessorDef );
            }
            
            commandLineArguments.add( vcProjectPath.getAbsolutePath() );
            
            return commandLineArguments;
        }
        
        private static final String CPPCHECK_XML_VERSION = "1";
        
        private File cppCheckPath;
        private File vcProjectPath;
        private CppCheckType cppCheckType = CppCheckType.all;
        private List<String> includeDirectories = new LinkedList<String>();
        private List<String> excludeDirectories = new LinkedList<String>();
        private List<String> preprocessorDefs = new LinkedList<String>();
    }
}

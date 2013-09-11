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
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;

/**
 * Mojo to execute MSBuild to build the required platform/configuration pairs.
 */
@Mojo( name = MSBuildMojo.MOJO_NAME,
        defaultPhase = LifecyclePhase.COMPILE )
@Execute( phase = LifecyclePhase.COMPILE )
public class MSBuildMojo extends AbstractMSBuildMojo 
{
    /**
     * The name this Mojo declares, also represents the goal.
     */
    public static final String MOJO_NAME = "build";

    /**
     * @throws MojoExecutionException if execution fails
     */
    @Override
    public final void execute() throws MojoExecutionException 
    {
        dumpConfiguration();
        validateForMSBuild();

        runMSBuild();
        findAndAttachArtifacts();
    }

    private void runMSBuild() throws MojoExecutionException
    {
        try
        {
            MSBuildExecutor msbuild = new MSBuildExecutor( getLog(), msbuildPath, projectFile );
            msbuild.setPlatforms( platforms );
            msbuild.setTargets( targets );
            if ( msbuild.execute() != 0 )
            {
                throw new MojoExecutionException(
                        "MSBuild execution failed, see log for details." );
            }
        }
        catch ( IOException ioe ) 
        {
            throw new MojoExecutionException(
                    "MSBUild execution failed", ioe );
        }
        catch ( InterruptedException ie )
        {
            throw new MojoExecutionException( "Interrupted waiting for "
                    + "MSBUild execution to complete", ie );
        }
    }

    private File getConfigurationOutputDirectory( BuildPlatform p, BuildConfiguration c ) throws MojoExecutionException
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
                result = new File( projectFile.getParentFile(), c.getName() );
                if ( result.exists() )
                {
                    getLog().debug( "Found output directory for Win32 at " + result.getAbsolutePath() );
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
                result = new File( projectFile.getParentFile(), p.getName() );
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
            getLog().error( exceptionMessage );
            getLog().error( "Looking for build output at " + result.getAbsolutePath() );
            throw new MojoExecutionException( exceptionMessage );
        }
    }

    /**
     * Works out the output filename (without the extension) from the project.
     * @return the name part of output files
     * @throws MojoExecutionException if called on a solution file or if the project filename has no extension
     */
    private String getOutputName() throws MojoExecutionException
    {
        if ( isSolution() )
        {
            throw new MojoExecutionException( "Internal error: Cannot determine single output name for a solutions" );
        }
        String projectFileName = projectFile.getName();
        if ( ! projectFileName.contains( "." ) )
        {
            throw new MojoExecutionException( "Project file name has no extension, please check your configuration" );
        }
        projectFileName = projectFileName.substring( 0, projectFileName.lastIndexOf( '.' ) );
        return projectFileName;
    }

    private void findAndAttachArtifacts() throws MojoExecutionException
    {
        getLog().info( "Attaching built artifacts" );
        if ( isSolution() )
        {
            attachSolutionArtifacts();
        }
        else
        {
            attachProjectArtifacts();
        }        
    }

    private void attachSolutionArtifacts() throws MojoExecutionException
    {
        // TODO: Header archives

        for ( BuildPlatform platform : platforms )
        {
            for ( BuildConfiguration configuration : platform.getConfigurations() )
            {
                final File archiveSource = getConfigurationOutputDirectory( platform, configuration );
                StringBuilder artifactName = new StringBuilder();
                artifactName.append( mavenProject.getArtifactId() ).append( "-" )
                            .append( mavenProject.getVersion() ).append( "-" )
                            .append( platform ).append( "-" )
                            .append( configuration )
                            .append( "." ).append( ZIP_EXTENSION );
                final File artifactFile = new File( 
                        mavenProject.getBuild().getDirectory(), 
                        artifactName.toString() );
    
                try
                {
                    zipArchiver.reset();
                    zipArchiver.setDestFile( artifactFile );
                    zipArchiver.addDirectory( archiveSource );
                    zipArchiver.createArchive();
                }
                catch ( IOException ioe )
                {
                    throw new MojoExecutionException( "Error creating archive", ioe );
                }
                String classifier = platform.getName() + "-" + configuration.getName();
                projectHelper.attachArtifact( mavenProject, ZIP_EXTENSION, classifier, artifactFile );
            }
        }        
    }

    private void attachProjectArtifacts() throws MojoExecutionException
    {
        // TODO: Header archives
        
        for ( BuildPlatform platform : platforms )
        {
            for ( BuildConfiguration configuration : platform.getConfigurations() )
            {
                StringBuilder outputFilename = new StringBuilder();
                outputFilename.append( getOutputName() )
                              .append( "." )
                              .append( mavenProject.getPackaging() );
                
                File artifactFile = new File( getConfigurationOutputDirectory( platform, configuration ),
                        outputFilename.toString() );
                if ( !artifactFile.exists() )
                {
                    String err = "Expected build output missing " + artifactFile.getAbsolutePath();
                    getLog().error( err );
                    throw new MojoExecutionException( err );
                }
                getLog().info( "Attaching file: " + artifactFile );
                if ( configuration.isPrimary() )
                {
                    mavenProject.getArtifact().setFile( artifactFile );
                }
                else
                {
                    String classifier = platform.getName() + "-" + configuration.getName();
                    projectHelper.attachArtifact( mavenProject, mavenProject.getPackaging(),
                            classifier, artifactFile );
                }
            }
        }
    }


    /**
     * The ZIP archiver.
     */
    @Component( role = org.codehaus.plexus.archiver.Archiver.class, hint = "zip" )
    private ZipArchiver zipArchiver;

    /**
     * The set of targets to build.
     */
    @Parameter(
            readonly = false,
            required = false )
    protected List<String> targets;
}

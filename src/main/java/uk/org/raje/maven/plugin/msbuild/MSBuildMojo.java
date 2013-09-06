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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

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
            msbuild.setConfiguration( configurations );
            msbuild.execute();
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

    private File getConfigurationOutputDirectory( String c )
    {
        // Assume that msbuild has created an output folder named after
        // the configuration that we built
        // If we add parsing of the solution/project file we can find this out for sure
        return new File( projectFile.getParentFile(), c );
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

        for ( String configuration : configurations )
        {
            final File archiveSource = getConfigurationOutputDirectory( configuration );
            StringBuilder artifactName = new StringBuilder();
            artifactName.append( mavenProject.getArtifactId() ).append( "-" )
                        .append( mavenProject.getVersion() ).append( "-" )
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
            projectHelper.attachArtifact( mavenProject, ZIP_EXTENSION, configuration, artifactFile );
        }
        
    }

    private void attachProjectArtifacts() throws MojoExecutionException
    {
        // TODO: Header archives
        
        String primaryArtifact = getPrimaryConfiguration();
        for ( String configuration : configurations )
        {
            StringBuilder outputFilename = new StringBuilder();
            outputFilename.append( getOutputName() )
                          .append( "." )
                          .append( mavenProject.getPackaging() );
            
            File artifactFile = new File( getConfigurationOutputDirectory( configuration ),
                    outputFilename.toString() );
            if ( !artifactFile.exists() )
            {
                String err = "Expected build output missing " + artifactFile.getAbsolutePath();
                getLog().error( err );
                throw new MojoExecutionException( err );
            }
            getLog().info( "Attaching file: " + artifactFile );
            if ( primaryArtifact.equals( configuration ) )
            {
                mavenProject.getArtifact().setFile( artifactFile );
            }
            else
            {
                projectHelper.attachArtifact( mavenProject, mavenProject.getPackaging(),
                        configuration, artifactFile );
            }
        }
    }

    /**
     * Get the configuration that will be treated as the main
     * output artifact of this build.
     * @return a configuration name from the list of configurations
     */
    private String getPrimaryConfiguration()
    {
        if ( configurations.contains( CONFIGURATION_RELEASE ) )
        {
            return CONFIGURATION_RELEASE;
        }
        if ( configurations.contains( CONFIGURATION_DEBUG ) )
        {
            return CONFIGURATION_DEBUG;
        }
        return configurations.get( 0 );
    }

    /**
     * The ZIP archiver.
     */
    @Component( role = org.codehaus.plexus.archiver.Archiver.class, hint = "zip" )
    private ZipArchiver zipArchiver;
}

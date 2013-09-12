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

        runMSBuild( targets );
        findAndAttachArtifacts();
    }

    /**
     * Works out the output filename (without the extension) from the project.
     * @return the name part of output files
     * @throws MojoExecutionException if called on a solution file or if the project filename has no extension
     */
    private String getOutputName() throws MojoExecutionException
    {
        if ( MSBuildPackaging.isSolution( mavenProject.getPackaging() ) )
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
        if ( MSBuildPackaging.isSolution( mavenProject.getPackaging() ) )
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
                final File archiveSource = MojoHelper.getConfigurationOutputDirectory( projectFile.getParentFile(), 
                        platform, configuration, getLog() );
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
                
                final File artifactDirectory = MojoHelper.getConfigurationOutputDirectory( projectFile.getParentFile(), 
                        platform, configuration, getLog() );
                final File artifactFile = new File( artifactDirectory, outputFilename.toString() );
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

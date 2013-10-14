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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProjectHelper;

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
     * @throws MojoFailureException if MSBuild return non-zero
     */
    @Override
    public final void doExecute() throws MojoExecutionException, MojoFailureException 
    {
        dumpConfiguration();
        validateForMSBuild();

        runMSBuild( targets, null );
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
        if ( MSBuildPackaging.isSolution( mavenProject.getPackaging() ) )
        {
            getLog().debug( "Not attaching any artifacts yet. "
                    + "Solution artifact bundle will be created in package phase." );
        }
        else
        {
            getLog().info( "Attaching built artifacts" );
            attachProjectArtifacts();
        }
    }

    private void attachProjectArtifacts() throws MojoExecutionException
    {
        for ( BuildPlatform platform : platforms )
        {
            for ( BuildConfiguration configuration : platform.getConfigurations() )
            {
                StringBuilder outputFilename = new StringBuilder();
                outputFilename.append( getOutputName() )
                              .append( "." )
                              .append( mavenProject.getPackaging() );
                
                final File artifactDirectory = getOutputDirectories( platform, configuration ).get( 0 );
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
     * Helper for attaching artifacts provided by the container. 
     */
    @Component
    protected MavenProjectHelper projectHelper;
}

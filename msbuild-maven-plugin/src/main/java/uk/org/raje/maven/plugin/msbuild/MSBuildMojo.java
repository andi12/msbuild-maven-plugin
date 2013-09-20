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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;
import uk.org.raje.maven.plugin.msbuild.parser.VCProject;

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
                final List<File> archiveSources = getOutputDirectories( platform, configuration );
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
                    for ( File archiveSource : archiveSources )
                    {
                        zipArchiver.addDirectory( archiveSource );
                    }
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
     * Determine the directories that msbuild will write output files to for a given platform and configuration.
     * If an outputDirectory is configured in the POM this will take precedence and be the only result.
     * @param p the BuildPlatform
     * @param c the BuildConfiguration
     * @return a List of File objects
     * @throws MojoExecutionException if an output directory cannot be determined or does not exist
     */
    private List<File> getOutputDirectories( BuildPlatform p, BuildConfiguration c )
            throws MojoExecutionException
    {
        List<File> result = new ArrayList<File>();
        
        // If there is a configured value use it
        File configured = c.getOutputDirectory();
        if ( configured != null )
        {
            result.add( configured );
        }
        else
        {
            List<VCProject> projects = parsedProjects( p, c );
            if ( projects.size() == 1 )
            {
                // probably a standalone project
                result.add( projects.get( 0 ).getOutputDirectory() );
            }
            else
            {
                // a solution
                for ( VCProject project : projects )
                {
                    boolean addResult = false;
                    if ( targets == null )
                    {
                        // building all targets, add all outputs
                        addResult = true;
                    }
                    else
                    {
                        // building select targets, only add ones we were asked for
                        if ( targets.contains( project.getTargetName() ) )
                        {
                            addResult = true;
                        }
                    }
    
                    if ( addResult && ! result.contains( project.getOutputDirectory() ) )
                    {
                        result.add( project.getOutputDirectory() );
                    }
                }
            }            
        }

        if ( result.size() < 1 )
        {
            String exceptionMessage = "Could not identify any output directories, configuration error?"; 
            getLog().error( exceptionMessage );
            throw new MojoExecutionException( exceptionMessage );
        }
        for ( File toTest: result )
        {
            // result will be populated, now check if it was created
            if ( ! toTest.exists() && ! toTest.isDirectory() )
            {
                String exceptionMessage = "Expected output directory was not created, configuration error?"; 
                getLog().error( exceptionMessage );
                getLog().error( "Looking for build output at " + toTest.getAbsolutePath() );
                throw new MojoExecutionException( exceptionMessage );
            }
        }
        return result;
    }


    /**
     * Helper for attaching artifacts provided by the container. 
     */
    @Component
    protected MavenProjectHelper projectHelper;

    /**
     * The ZIP archiver.
     */
    @Component( role = org.codehaus.plexus.archiver.Archiver.class, hint = "zip" )
    private ZipArchiver zipArchiver;
}

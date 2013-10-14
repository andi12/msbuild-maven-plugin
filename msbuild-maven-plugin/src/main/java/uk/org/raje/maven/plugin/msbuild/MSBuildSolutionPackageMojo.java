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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;

/**
 * Mojo to create ZIP packages of the solution build artifacts.
 */
@Mojo( name = MSBuildSolutionPackageMojo.MOJO_NAME,
        defaultPhase = LifecyclePhase.PACKAGE )
@Execute( phase = LifecyclePhase.PACKAGE )
public class MSBuildSolutionPackageMojo extends AbstractMSBuildPluginMojo
{
    /**
     * The name this Mojo declares, also represents the goal.
     */
    public static final String MOJO_NAME = "solution-package";

    /**
     * The file extension for zip archives.
     */
    public static final String ZIP_EXTENSION = "zip";

    @Override
    public final void doExecute() throws MojoExecutionException, MojoFailureException 
    {
        // TODO: Header archives - see Issue #1

        if ( MSBuildPackaging.isSolution( mavenProject.getPackaging() ) )
        {
            buildAndattachSolutionArtifacts();
        }
        else
        {
            getLog().warn( "Packaging is not " + MSBuildPackaging.MSBUILD_SOLUTION + ", no packaging to do." );
        }
        
    }

    private void buildAndattachSolutionArtifacts() throws MojoExecutionException
    {
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
                    getLog().debug( "Adding outputs to archive..." );
                    for ( File archiveSource : archiveSources )
                    {
                        getLog().debug( "    " + archiveSource );
                        zipArchiver.addDirectory( archiveSource );
                    }
                    getLog().debug( "Done adding outputs to archive." );
                    zipArchiver.createArchive();
                }
                catch ( IOException ioe )
                {
                    throw new MojoExecutionException( "Error creating archive", ioe );
                }
                String classifier = platform.getName() + "-" + configuration.getName();
                projectHelper.attachArtifact( mavenProject, ZIP_EXTENSION, classifier, artifactFile );
                getLog().info( "Attached artifact " + artifactFile );
            }
        }        
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

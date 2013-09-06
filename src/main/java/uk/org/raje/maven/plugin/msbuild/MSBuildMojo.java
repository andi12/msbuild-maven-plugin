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
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

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

    private void findAndAttachArtifacts()
    {
        // TODO: 
        // Solutions result in a zip - need to zip up 'configurations' directories
        // Projects results in an exe or lib depending on packaging
        //   Attach files with appropriate classifiers 
        // TODO: Work out the exe to attach
        boolean attachedMainArtifact = false;
        for ( String configuration : configurations )
        {
            StringBuilder artifactFilePath = new StringBuilder();
            artifactFilePath.append( projectFile.getParent() ).append( File.separator )
                            .append( configuration ).append( File.separator );
            String exeName = projectFile.getName();
            exeName = exeName.substring( 0, exeName.lastIndexOf( '.' ) );
            artifactFilePath.append( exeName ).append( "." ).append( EXE_EXTENSION );
            
            File artifactFile = new File( artifactFilePath.toString() );
            getLog().info( "Attaching file: " + artifactFile );
            if ( CONFIGURATION_RELEASE.equals( configuration ) )
            {
                mavenProject.getArtifact().setFile( artifactFile );
                attachedMainArtifact = true;
            }
            else
            {
                projectHelper.attachArtifact( mavenProject, EXE_EXTENSION, configuration, artifactFile );
            }
        }
        // TODO: What if no main artifact yet?
        
    }

    /**
     * Log out configuration values at DEBUG.
     */
    private void dumpConfiguration()
    {
        getLog().info( "MSBuild path: " + msbuildPath );
        getLog().info( "Platforms: " + platforms );
        getLog().info( "Configurations: " + configurations );
    }

}

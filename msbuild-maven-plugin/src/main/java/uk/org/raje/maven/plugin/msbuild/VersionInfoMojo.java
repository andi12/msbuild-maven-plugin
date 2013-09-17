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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;

/**
 * Mojo to place an rc file containing version information generated from Maven into the specified targets.
 */
@Mojo( name = VersionInfoMojo.MOJO_NAME,
defaultPhase = LifecyclePhase.GENERATE_RESOURCES )
@Execute( phase = LifecyclePhase.GENERATE_RESOURCES )
public class VersionInfoMojo extends AbstractMSBuildPluginMojo
{
    /**
     * The name this Mojo declares, also represents the goal.
     */
    public static final String MOJO_NAME = "version-info";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if ( ! MSBuildPackaging.isValid( mavenProject.getPackaging() ) )
        {
            getLog().info( "Packaging not valid for MSBuild. Skipping generating version-info.rc" );
            return;
        }
        if ( versionInfo.skip() )
        {
            getLog().info( "skipVersionInfo is true. Skipping generating version-info.rc" );
            return;
        }
        if ( versionInfo.companyName() == null )
        {
            getLog().warn( "Missing <companyName>. Skipping generating version-info.rc" );
            return;
        }
        
        MojoHelper.validateProjectFile( mavenProject.getPackaging(), projectFile, getLog() );

        addProjectProperties();
        
        try
        {
            File versionInfoSrc = writeVersionInfoTemplateToTempFile();
            final File versionInfoDest = new File( projectFile.getParentFile(), "version-info.rc" ); 
    
            fileFiltering.copyFile( versionInfoSrc, versionInfoDest, true, mavenProject, 
                    Collections.<String> emptyList(), true, "UTF-8", null );
            
            versionInfoSrc.delete();
        }
        catch ( MavenFilteringException mfe )
        {
            String msg = "Error replacing properties in version file";
            getLog().error( msg );
            throw new MojoExecutionException( msg, mfe );
        }
    }

    /**
     * Add properties to the project that are replaced.
     */
    private void addProjectProperties() throws MojoExecutionException
    {
        Properties projectProps = mavenProject.getProperties();
        
        projectProps.setProperty( PROPERTY_NAME_COMPANY, versionInfo.companyName() );
        projectProps.setProperty( PROPERTY_NAME_COPYRIGHT, versionInfo.copyright() );

        projectProps.setProperty( PROPERTY_NAME_VERSION_MAJOR, "0" );
        projectProps.setProperty( PROPERTY_NAME_VERSION_MINOR, "0" );
        projectProps.setProperty( PROPERTY_NAME_VERSION_INCREMENTAL, "0" );
        projectProps.setProperty( PROPERTY_NAME_VERSION_BUILD, "0" );
        String version = mavenProject.getVersion();
        if ( version != null && version.length() > 0 )
        {
            ArtifactVersion artifactVersion = new DefaultArtifactVersion( version );
            if ( version.equals( artifactVersion.getQualifier() ) )
            {
                String msg = "Unable to parse the version string, please use standard maven version format.";
                getLog().error( msg );
                throw new MojoExecutionException( msg );
            }
            projectProps.setProperty( PROPERTY_NAME_VERSION_MAJOR, 
                    String.valueOf( artifactVersion.getMajorVersion() ) );
            projectProps.setProperty( PROPERTY_NAME_VERSION_MINOR, 
                    String.valueOf( artifactVersion.getMinorVersion() ) );
            projectProps.setProperty( PROPERTY_NAME_VERSION_INCREMENTAL, 
                    String.valueOf( artifactVersion.getIncrementalVersion() ) );
            projectProps.setProperty( PROPERTY_NAME_VERSION_BUILD, 
                    String.valueOf( artifactVersion.getBuildNumber() ) );
        }
        else
        {
            getLog().warn( "Missing version for project. Version parts will be set to 0" );
        }
    }

    /**
     * Write the default .rc template file to a temporary file and return it
     * @return a File pointing to the temporary file
     * @throws MojoExecutionException if there is an IOException
     */
    private File writeVersionInfoTemplateToTempFile() throws MojoExecutionException
    {
        try 
        {
            final File versionInfoSrc = File.createTempFile( "msbuild-maven-plugin_" + MOJO_NAME, null );
    
            InputStream is = getClass().getResourceAsStream( DEFAULT_VERSION_INFO_TEMPLATE );
            FileOutputStream dest = new FileOutputStream( versionInfoSrc );
            byte[] buffer = new byte[1024];
            int read = -1;
            while ( ( read = is.read( buffer ) ) != -1 )
            {
                dest.write( buffer, 0, read );
            }
            dest.close();
            
            return versionInfoSrc;
        } 
        catch ( IOException ioe )
        {
            String msg = "Failed to create temporary version file";
            getLog().error( msg, ioe );
            throw new MojoExecutionException( msg, ioe );
        }
    }

    private static final String DEFAULT_VERSION_INFO_TEMPLATE = "/DefaultVersionInfoTemplate.rc";

    private static final String PROPERTY_NAME_COMPANY = MOJO_NAME + ".companyname";
    private static final String PROPERTY_NAME_COPYRIGHT = MOJO_NAME + ".copyright";

    private static final String PROPERTY_NAME_VERSION_MAJOR = MOJO_NAME + ".majorVersion";
    private static final String PROPERTY_NAME_VERSION_MINOR = MOJO_NAME + ".minorVersion";
    private static final String PROPERTY_NAME_VERSION_INCREMENTAL = MOJO_NAME + ".incrementalVersion";
    private static final String PROPERTY_NAME_VERSION_BUILD = MOJO_NAME + ".buildNumber";

    // THIS IS NOT WORKING, luckily we don't need the MavenSession at present
    //@Parameter( 
    //        defaultValue = "${session}", 
    //        required = true, 
    //        readonly = true )
    //protected MavenSession mavenSession;
    
    @Component(
            role = MavenFileFilter.class,
            hint = "default" )
    private MavenFileFilter fileFiltering;
}

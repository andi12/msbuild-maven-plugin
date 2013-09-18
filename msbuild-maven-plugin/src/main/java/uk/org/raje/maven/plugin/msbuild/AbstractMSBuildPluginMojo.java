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
import java.lang.reflect.Field;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;
import uk.org.raje.maven.plugin.msbuild.configuration.CppCheckConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.CxxTestConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.VersionInfoConfiguration;

/**
 * Abstract base class for the msbuild-maven-plugin which defines all configuration properties exposed.
 */
public abstract class AbstractMSBuildPluginMojo extends AbstractMojo
{
    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException
    {
        // Fix up configuration
        // This is done with the following hard coded fixes for parameters that
        // we want to be able to pull from -D's or settings.xml but are stored
        // in configuration sub-classes.
        try
        {
            if ( cppCheckPath != null )
            {
                Field cppCheckPathField = CppCheckConfiguration.class.getDeclaredField( "cppCheckPath" );
                cppCheckPathField.setAccessible( true );
                cppCheckPathField.set( cppCheck, cppCheckPath );
                getLog().debug( "Found property for cppcheck.path, using this value" );
            }
            if ( cxxTestHome != null )
            {
                Field cxxTestPathField = CxxTestConfiguration.class.getDeclaredField( "cxxTestHome" );
                cxxTestPathField.setAccessible( true );
                cxxTestPathField.set( cxxTest, cxxTestHome );
                getLog().debug( "Found property for cxxtest.home, using this value" );
           }
        }
        catch ( NoSuchFieldException nsfe )
        {
            throw new MojoFailureException( "Internal error, please contact the Mojo developer", nsfe );
        }
        catch ( IllegalAccessException iae )
        {
            throw new MojoFailureException( "Internal error, please contact the Mojo developer", iae );
        }

        // Configuration fixed, call child to do real work
        doExecute();
    }

    /**
     * Actually perform the work of this Mojo now the configuration has been fixed.
     * @see AbstractMojo#execute
     */
    protected abstract void doExecute() throws MojoExecutionException, MojoFailureException;

    /**
     * The MavenProject for the current build.
     */
    @Parameter( defaultValue = "${project}" )
    protected MavenProject mavenProject;

    /**
     * The project (.vcxproj) or solution (.sln) file to build.
     */
    @Parameter(
            readonly = false,
            required = true )
    protected File projectFile;

    /**
     * The set of platforms and configurations to build.
     */
    @Parameter(
            readonly = false,
            required = false )
    protected List<BuildPlatform> platforms;

    /**
     * The set of targets to build.
     */
    @Parameter(
            readonly = false,
            required = false )
    protected List<String> targets;

    /**
     * The path to MSBuild.
     */
    @Parameter(
            property = "msbuild.path",
            readonly = false,
            required = true )
    protected File msbuildPath;

    /**
     * Configure the version-info Mojo.
     */
    @Parameter
    protected VersionInfoConfiguration versionInfo = new VersionInfoConfiguration();

    /**
     * Configure the CppCheck Mojo.
     */
    @Parameter
    protected CppCheckConfiguration cppCheck = new CppCheckConfiguration();

    /**
     * Configure the CxxTest Mojo.
     */
    @Parameter
    protected CxxTestConfiguration cxxTest = new CxxTestConfiguration();

    /**
     * This parameter only exists to pickup a -D property or property in settings.xml
     * @see CppCheckConfiguration#cppCheckPath
     */
    @Parameter(
            property = "cppcheck.path",
            readonly = true,
            required = false )
    private File cppCheckPath;

    /**
     * This parameter only exists to pickup a -D property or property in settings.xml
     * @see CxxTestConfiguration#cxxTestHome
     */
    @Parameter( 
            property = "cxxtest.home", 
            readonly = true, 
            required = false )
    private File cxxTestHome;
}

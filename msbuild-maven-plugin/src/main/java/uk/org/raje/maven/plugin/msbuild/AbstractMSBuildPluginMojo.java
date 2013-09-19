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
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.security.InvalidParameterException;
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

    protected boolean isCxxTestEnabled( String stepName ) 
    {
        if ( cxxTest.skip() )
        {
            getLog().info( CXXTEST_SKIP_MESSAGE + " " + stepName + ", 'skip' set to true in the " 
                    + CxxTestConfiguration.CXXTEST_NAME + " configuration." );
            
            return false;
        }
        
        if ( cxxTest.getCxxTestHome() == null ) 
        {
            getLog().info( CXXTEST_SKIP_MESSAGE + ", path to " + CxxTestConfiguration.CXXTEST_NAME + " not set." );
            return false;
        }
        
        return true;
    }

    protected void validateCxxTestConfiguration() throws MojoExecutionException, MojoFailureException 
    {
        if ( !getCxxTestPython2Home().isDirectory() )
        {
            throw new MojoExecutionException( "Could not find the Python 2 scripts for " 
                    + CxxTestConfiguration.CXXTEST_NAME + " at " + getCxxTestPython2Home(), 
                    new FileNotFoundException( getCxxTestPython2Home().getAbsolutePath() ) );
        }
        
        try 
        {
            MojoHelper.validateToolPath( new File( getCxxTestPython2Home(), "cxxtest/cxxtestgen.py" ), 
                    CxxTestConfiguration.CXXTEST_HOME, CxxTestConfiguration.CXXTEST_NAME, getLog() );
        }
        catch ( FileNotFoundException fnfe )
        {
            throw new MojoExecutionException( CxxTestConfiguration.CXXTEST_NAME 
                    + " could not be found at " + fnfe.getMessage() + ". "
                    + "You need to configure it in the plugin configuration section in the "
                    + "POM file using <cxxTestHome>...</cxxTestHome> "
                    + "or <properties><cxxtest.home>...</cxxtest.home></properties>; "
                    + "alternatively, you can use the command-line parameter -Dcxxtest.home=... "
                    + "or set the environment variable " + CxxTestConfiguration.CXXTEST_HOME, fnfe );
        }
        
        if ( cxxTest.getTestTargets() == null || cxxTest.getTestTargets().size() == 0 )
        {
            throw new MojoExecutionException( "You must specify at least one test target. If you want to skip "
                    + "running the tests, please set 'skip' to true in the " + CxxTestConfiguration.CXXTEST_NAME 
                    + " configuration.", new InvalidParameterException( "testTargets" ) );
        }
        
        MojoHelper.validateProjectFile( mavenProject.getPackaging(), projectFile, getLog() );
        platforms = MojoHelper.validatePlatforms( platforms );
    }

    protected File getCxxTestPython2Home() 
    {
        return new File( cxxTest.getCxxTestHome(), "python" );
    }
    
    protected boolean isCppCheckEnabled() 
    {
        if ( cppCheck.skip() )
        {
            getLog().info( CPPCHECK_SKIP_MESSAGE + ", 'skip' set to true in the " + CppCheckConfiguration.CPPCHECK_NAME
                    + " configuration." );
            
            return false;
        }
        
        if ( cppCheck.getCppCheckPath() == null ) 
        {
            getLog().info( CPPCHECK_SKIP_MESSAGE + ", path to " + CppCheckConfiguration.CPPCHECK_NAME + " not set." );
            return false;
        }        
        
        return true;
    }

    protected void validateCppCheckConfiguration() throws MojoExecutionException, MojoFailureException 
    {
        try 
        {
            MojoHelper.validateToolPath( cppCheck.getCppCheckPath(), CppCheckConfiguration.CPPCHECK_PATH_ENVVAR, 
                    CppCheckConfiguration.CPPCHECK_NAME, getLog() );
        }
        catch ( FileNotFoundException fnfe )
        {
            throw new MojoExecutionException( CppCheckConfiguration.CPPCHECK_NAME 
                    + "could not be found at " + fnfe.getMessage() + ". "
                    + "You need to configure it in the plugin configuration section in the "
                    + "POM file using <cppCheckPath>...</cppCheckPath> "
                    + "or <properties><cppcheck.path>...</cppcheck.path></properties>; "
                    + "alternatively, you can use the command-line parameter -Dcppcheck.path=... "
                    + "or set the environment variable " + CppCheckConfiguration.CPPCHECK_PATH_ENVVAR, fnfe );
        }
        
        MojoHelper.validateProjectFile( mavenProject.getPackaging(), projectFile, getLog() );
        platforms = MojoHelper.validatePlatforms( platforms );
    }

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
    
    private static final String CXXTEST_SKIP_MESSAGE = "Skipping test";
    private static final String CPPCHECK_SKIP_MESSAGE = "Skipping static code analysis";
}

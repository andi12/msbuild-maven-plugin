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
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.xml.sax.SAXException;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;
import uk.org.raje.maven.plugin.msbuild.configuration.CppCheckConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.CxxTestConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.SonarConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.VersionInfoConfiguration;
import uk.org.raje.maven.plugin.msbuild.parser.VCProject;
import uk.org.raje.maven.plugin.msbuild.parser.VCProjectHolder;

/**
 * Abstract base class for the msbuild-maven-plugin which defines all configuration properties exposed.
 */
public abstract class AbstractMSBuildPluginMojo extends AbstractMojo
{
    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException
    {
        VCPROJECTHOLDER_LOGGING_HANDLER.setMavenLogger( getLog() );

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
     * Check that we have a valid project or solution file.
     * @throws MojoExecutionException if the specified projectFile is invalid.
     */
    protected void validateProjectFile() 
            throws MojoExecutionException
    {
        if ( projectFile != null
                && projectFile.exists()
                && projectFile.isFile() )
        {
            getLog().debug( "Project file validated at " + projectFile );

            boolean solutionFile = projectFile.getName().toLowerCase().endsWith( "." + SOLUTION_EXTENSION ); 
            if ( ( MSBuildPackaging.isSolution( mavenProject.getPackaging() ) && ! solutionFile )
                    || ( ! MSBuildPackaging.isSolution( mavenProject.getPackaging() ) && solutionFile ) )
            {
                // Solution packaging defined but the projectFile is not a .sln
                String msg = "Packaging doesn't match project file type. "
                        + "If you specify a solution file then packaging must be " + MSBuildPackaging.MSBUILD_SOLUTION;
                getLog().error( msg );
                throw new MojoExecutionException( msg );
            }
            return;
        }
        String prefix = "Missing projectFile";
        if ( projectFile != null )
        {
            prefix = ". The specified projectFile '" + projectFile
                    + "' is not valid";
        }
        throw new MojoExecutionException( prefix
                + ", please check your configuration" );
    }

    /**
     * Compute the relative path portion from a path and a base directory.
     * If basedir and target are the same "." is returned.
     * For example: Given C:\foo\bar and C:\foo\bar\baz this method will return baz
     * @param basedir the base directory
     * @param target the path to express as relative to basedir
     * @return the relative portion of the path between basedir and target
     * @throws MojoExecutionException if the target is not basedir or a subpath of basedir
     */
    protected File getRelativeFile( File basedir, File target ) throws MojoExecutionException
    {
        String basedirStr = basedir.getPath();
        String targetDirStr = target.getPath();
        
        if ( targetDirStr.equals( basedirStr ) )
        {
            return new File( "." );
        }
        else if ( targetDirStr.startsWith( basedirStr + File.separator ) ) // add slash to ensure directory
        {
            return new File( targetDirStr.substring( basedirStr.length() + 1 ) ); // + slash char
        }
        throw new MojoExecutionException( "Unable to relativize " + targetDirStr + " to " + basedir );
    }

    /**
     * Return project configurations for the specified platform and configuration.
     * @param platform the platform to parse for
     * @param configuration the configuration to parse for
     * @return a list of VCProject objects containing configuration for the specified platform and configuration
     * @throws MojoExecutionException if parsing fails
     */
    protected List<VCProject> getParsedProjects( BuildPlatform platform, BuildConfiguration configuration ) 
            throws MojoExecutionException
    {
        VCProjectHolder vcProjectHolder = VCProjectHolder.getVCProjectHolder( projectFile, 
                MSBuildPackaging.isSolution( mavenProject.getPackaging() ) );
        
        try
        {
            return vcProjectHolder.getParsedProjects( platform.getName(), configuration.getName() );
        }
        catch ( FileNotFoundException fnfe ) 
        {
            throw new MojoExecutionException( "Could not find file " + projectFile, fnfe );
        }
        catch ( IOException ioe ) 
        {
            throw new MojoExecutionException( "I/O error while parsing file " + projectFile, ioe );
        }
        catch ( SAXException se ) 
        {
            throw new MojoExecutionException( "Syntax error while parsing file " + projectFile, se );
        }
        catch ( ParserConfigurationException pce )
        {
            throw new MojoExecutionException( "XML parser configuration exception ", pce );
        }
        catch ( ParseException pe ) 
        {
            throw new MojoExecutionException( "Syntax error while parsing solution file " + projectFile, pe );
        }
    }

    /**
     * Return project configurations for the specified platform and configuration filtered by name using the specified 
     * Pattern.
     * @param platform the platform to parse for
     * @param configuration the configuration to parse for
     * @param filterRegex a Pattern to use to filter the projects
     * @return a list of VCProject objects containing configuration for the specified platform and configuration
     * @throws MojoExecutionException if parsing fails
     */
    protected List<VCProject> getParsedProjects( BuildPlatform platform, BuildConfiguration configuration, 
            Pattern filterRegex ) throws MojoExecutionException
    {
        List<VCProject> unfiltered = getParsedProjects( platform, configuration );
        List<VCProject> filteredList = new ArrayList<VCProject>( unfiltered.size() );
        for ( VCProject p : unfiltered )
        {
            if ( filterRegex == null )
            {
                filteredList.add( p );
            }
            else
            {
                Matcher prjExcludeMatcher = filterRegex.matcher( p.getName() );
                if ( ! prjExcludeMatcher.matches() )
                {
                    filteredList.add( p );
                }
            }
        }
        return filteredList;
    }    

    /**
     * Return the project configuration for the specified target, platform and configuration
     * Note: This is only valid for solutions as target names don't apply for a standalone project file
     * @param targetName the target to look for
     * @param platform the platform to parse for
     * @param configuration the configuration to parse for
     * @return the VCProject for the specified target
     * @throws MojoExecutionException if the requested project cannot be identified
     */
    protected VCProject getParsedProject( String targetName, BuildPlatform platform, BuildConfiguration configuration )
            throws MojoExecutionException
    {
        List<VCProject> projects = getParsedProjects( platform, configuration );
        for ( VCProject project : projects )
        {
            if ( targetName.equals( project.getTargetName() ) )
            {
                return project;
            }
        }
        throw new MojoExecutionException( "Target '" + targetName + "' not found in project files" );
    }

    /**
     * Determine whether CxxTest is enabled by the configuration
     * @param stepName the string to use in log messages to describe the process being attempted
     * @param quiet does not output any log messages ({@code stepName} may be {@code null} in this case)
     * @return true if CxxTest is configured, false otherwise
     * @throws MojoExecutionException if CxxTest is configured for a project not a solution
     */
    protected boolean isCxxTestEnabled( String stepName, boolean quiet ) throws MojoExecutionException
    {
        if ( ! MSBuildPackaging.isSolution( mavenProject.getPackaging() )
            && ( cxxTest.getTestTargets() != null ) )
        {
            String msg = "CxxTest is only supported for solution (.sln) files!";
            getLog().error( msg );
            throw new MojoExecutionException( msg );
        }

        if ( cxxTest.skip() )
        {
            if ( ! quiet )
            {
                getLog().info( CXXTEST_SKIP_MESSAGE + " " + stepName + ", 'skip' set to true in the " 
                        + CxxTestConfiguration.CXXTEST_NAME + " configuration." );
            }
            
            return false;
        }
        
        if ( cxxTest.getCxxTestHome() == null ) 
        {
            if ( ! quiet )
            {
                getLog().info( CXXTEST_SKIP_MESSAGE + " " + stepName + ", path to " 
                        + CxxTestConfiguration.CXXTEST_NAME + " not set." );
            }
            
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
        
        validateProjectFile();
        platforms = MojoHelper.validatePlatforms( platforms );
    }

    protected File getCxxTestPython2Home() 
    {
        return new File( cxxTest.getCxxTestHome(), "python" );
    }
    
    protected boolean isCppCheckEnabled( boolean quiet ) 
    {
        if ( cppCheck.skip() )
        {
            if ( ! quiet )
            {
                getLog().info( CPPCHECK_SKIP_MESSAGE + ", 'skip' set to true in the " 
                        + CppCheckConfiguration.CPPCHECK_NAME + " configuration." );
            }
            
            return false;
        }
        
        if ( cppCheck.getCppCheckPath() == null ) 
        {
            if ( ! quiet )
            {
                getLog().info( CPPCHECK_SKIP_MESSAGE + ", path to " 
                        + CppCheckConfiguration.CPPCHECK_NAME + " not set." );
            }
            
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
        
        validateProjectFile();
        platforms = MojoHelper.validatePlatforms( platforms );
    }
    
    protected boolean isSonarEnabled( boolean quiet ) throws MojoExecutionException
    {
        if ( sonar.skip() )
        {
            if ( ! quiet )
            {
                getLog().info( SONAR_SKIP_MESSAGE + ", 'skip' set to true in the " + SonarConfiguration.SONAR_NAME
                        + " configuration." );
            }
            
            return false;
        }
        
        validateProjectFile();
        platforms = MojoHelper.validatePlatforms( platforms );
        
        return true;
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
     * The value for 'maxcpucount' to pass to MSBuild.
     * Default value of -1 results in MSBuild being invoked with '/maxcpucount'.
     */
    @Parameter(
            defaultValue = "-1",
            readonly = false,
            required = false )
    protected int msbuildMaxCpuCount = -1;

    /**
     * The system includes to use.
     * A semi-colon separated list of paths.
     * Currently only used to generate Sonar configuration.
     */
    @Parameter(
            property = "msbuild.includes",
            readonly = false,
            required = false )
    protected String msbuildSystemIncludes;

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
     * Configure the Sonar Mojo.
     */
    @Parameter
    protected SonarConfiguration sonar = new SonarConfiguration();
    
    /**
     * The file extension for solution files.
     */
    private static final String SOLUTION_EXTENSION = "sln";

    private static final String CXXTEST_SKIP_MESSAGE = "Skipping test";
    private static final String CPPCHECK_SKIP_MESSAGE = "Skipping static code analysis";
    private static final String SONAR_SKIP_MESSAGE = "Skipping Sonar analysis";
    
    /**
     * This handler capture standard Java logging produced by {@link VCProjectHolder} and relays it to the Maven logger
     * provided by the Mojo. It needs to be static to prevent duplicate log output. 
     * @see {@link LoggingHandler#LoggingHandler(Class klass)} 
     */
    private static final LoggingHandler VCPROJECTHOLDER_LOGGING_HANDLER = new LoggingHandler( VCProjectHolder.class );
    
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

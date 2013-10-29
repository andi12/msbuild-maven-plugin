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
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.xml.sax.SAXException;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;
import uk.org.raje.maven.plugin.msbuild.configuration.CppCheckConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.CxxTestConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.SonarConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.VeraConfiguration;
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
        VCPROJECT_HOLDER_LOG_HANDLER.setLog( getLog() );
        
        // Fix up configuration
        // This is done with the following fixes for parameters that we want to be able to pull 
        // from -D's, settings.xml or environment variables but are stored in configuration sub-classes.
        // Unfortunately the @Parameter 'property' attribute doesn't work for configuration sub-classes.
        cppCheck.setCppCheckPath(
                findConfiguredToolPath( "CppCheck executable path", 
                        cppCheck.getCppCheckPath(), 
                        CppCheckConfiguration.PATH_PROPERTY,  
                        CppCheckConfiguration.PATH_ENVVAR ) );
        
        vera.setVeraHome(
                findConfiguredToolPath( "Vera++ home directory", 
                        vera.getVeraHome(),
                        VeraConfiguration.HOME_PROPERTY,  
                        VeraConfiguration.HOME_ENVVAR ) );
        
        cxxTest.setCxxTestHome(
                findConfiguredToolPath( "CxxTest home directory", 
                        cxxTest.getCxxTestHome(), 
                        CxxTestConfiguration.HOME_PROPERTY,  
                        CxxTestConfiguration.HOME_ENVVAR ) );
        
        if ( "true".equalsIgnoreCase( findProperty( CxxTestConfiguration.SKIP_TESTS_PROPERTY ) ) )
        {
            cxxTest.setSkipTests( true );
        }
        
        if ( "true".equalsIgnoreCase( findProperty( CxxTestConfiguration.IGNORE_FAILURE_PROPERTY ) ) )
        {
            cxxTest.setTestFailureIgnore( true );
        }

        // Configuration fixed, call child to do real work
        doExecute();
    }

    /**
     * Find a configuration for the specified tool path.
     * The following precedence is observed: System property, POM value, Project property, Environment variable
     * @param toolName the name of the tool being sought, used for logging
     * @param pomValue the value found in the POM
     * @param prop the property name
     * @param envvar the environment variable name
     * @return the value determined or null if not found
     */
    private File findConfiguredToolPath( String toolName, File pomValue, String prop, String envvar )
    {
        String systemPropertyValue = System.getProperty( prop );
        if ( systemPropertyValue != null && !systemPropertyValue.isEmpty() )
        {
            getLog().debug( toolName + " found in " + prop + " system property" );
            return new File( systemPropertyValue );
        }

        // No system property, we'll use the pom configured value if provided
        File result = pomValue;
        
        if ( result == null )
        {
            // Try project property ...
            String projectPropertyValue = mavenProject.getProperties().getProperty( prop );
            if ( projectPropertyValue != null && !projectPropertyValue.isEmpty() )
            {            
                getLog().debug( toolName + " found in " + prop + " property" );
                result = new File( projectPropertyValue );
            }
            else
            {
                // Try environment variable ...
                String envValue = System.getenv( envvar );
                if ( envValue != null && !envValue.isEmpty() )
                {
                    getLog().debug( toolName + " found in environment variable " + envvar );
                    result = new File( envValue );
                }
            }
        }        
        
        return result;
    }

    /**
     * Look for a value for the specified property in System properties then project properties. 
     * @param prop the property name
     * @return the value or null if not found
     */
    private String findProperty( String prop )
    {
        String result = System.getProperty( prop );
        if ( result == null )
        {
            result = mavenProject.getProperties().getProperty( prop );
        }
        return result;
    }

    /**
     * Actually perform the work of this Mojo now the configuration has been fixed.
     * @see AbstractMojo#execute
     * @throws MojoExecutionException if an unexpected problem occurs.
     * @throws MojoFailureException if an expected problem (such as a compilation failure) occurs.
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
     * @param baseDir the base directory
     * @param targetFile the path to express as relative to basedir
     * @return the relative portion of the path between basedir and target
     * @throws IOException if the target is not basedir or a subpath of basedir
     */
    protected File getRelativeFile( File baseDir, File targetFile ) throws IOException
    {
        String baseDirStr = baseDir.getPath();
        String targetDirStr = targetFile.getPath();
        
        if ( targetDirStr.equals( baseDirStr ) )
        {
            return new File( "." );
        }
        else if ( targetDirStr.startsWith( baseDirStr + File.separator ) ) // add slash to ensure directory
        {
            return new File( targetDirStr.substring( baseDirStr.length() + 1 ) ); // + slash char
        }
        
        throw new IOException( "Unable to relativize " + targetDirStr + " to " + baseDir );
    }

    /**
     * Generate a list of source files in the project directory and sub-directories
     * @param vcProject the parsed project
     * @param includeHeaders set to true to include header files (*.h and *.hpp)
     * @return a list of abstract paths representing each source file
     */
    protected List<File> getProjectSources( VCProject vcProject, boolean includeHeaders ) 
    {
        final DirectoryScanner directoryScanner = new DirectoryScanner();
        List<String> sourceFilePatterns = new ArrayList<String>();
        List<File> sourceFiles = new ArrayList<File>();
        
        sourceFilePatterns.add( "**\\*.c" );
        sourceFilePatterns.add( "**\\*.cpp" );
        
        if ( includeHeaders )
        {
            sourceFilePatterns.add( "**\\*.h" );
            sourceFilePatterns.add( "**\\*.hpp" );
        }
        
        directoryScanner.setIncludes( sourceFilePatterns.toArray( new String[0] ) );
        directoryScanner.setBasedir( vcProject.getFile().getParentFile() );
        directoryScanner.scan();
        
        for ( String fileName : directoryScanner.getIncludedFiles() )
        {
            sourceFiles.add( new File( vcProject.getFile().getParentFile(), fileName ) );
        }
        
        return sourceFiles;
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
        Map<String, String> envVariables = new HashMap<String, String>();
        
        if ( isCxxTestEnabled( null, true ) )
        {
            envVariables.put( CxxTestConfiguration.HOME_ENVVAR, cxxTest.getCxxTestHome().getPath() );
        }
        
        VCProjectHolder vcProjectHolder = VCProjectHolder.getVCProjectHolder( projectFile, 
                MSBuildPackaging.isSolution( mavenProject.getPackaging() ), envVariables );
        
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
            String filterRegex ) throws MojoExecutionException
    {
        Pattern filterPattern = null;
        List<VCProject> filteredList = new ArrayList<VCProject>();
        
        if ( filterRegex != null )
        {
            filterPattern = Pattern.compile( filterRegex );
        }
        
        for ( VCProject vcProject : getParsedProjects( platform, configuration ) )
        {
            if ( filterPattern == null )
            {
                filteredList.add( vcProject );
            }
            else
            {
                Matcher prjExcludeMatcher = filterPattern.matcher( vcProject.getName() );
                
                if ( ! prjExcludeMatcher.matches() )
                {
                    filteredList.add( vcProject );
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
     * Determine the directories that msbuild will write output files to for a given platform and configuration.
     * If an outputDirectory is configured in the POM this will take precedence and be the only result.
     * @param p the BuildPlatform
     * @param c the BuildConfiguration
     * @return a List of File objects
     * @throws MojoExecutionException if an output directory cannot be determined or does not exist
     */
    protected List<File> getOutputDirectories( BuildPlatform p, BuildConfiguration c )
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
            List<VCProject> projects = getParsedProjects( p, c );
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
     * Determine whether CppCheck is enabled by the configuration
     * @param quiet set to true to suppress logging
     * @return true if CppCheck is enabled, false otherwise
     */
    protected boolean isCppCheckEnabled( boolean quiet ) 
    {
        if ( cppCheck.skip() )
        {
            if ( ! quiet )
            {
                getLog().info( CppCheckConfiguration.SKIP_MESSAGE 
                        + ", 'skip' set to true in the " + CppCheckConfiguration.TOOL_NAME + " configuration." );
            }
            
            return false;
        }
        
        if ( cppCheck.getCppCheckPath() == null ) 
        {
            if ( ! quiet )
            {
                getLog().info( CppCheckConfiguration.SKIP_MESSAGE 
                        + ", path to " + CppCheckConfiguration.TOOL_NAME + " not set." );
            }
            
            return false;
        }        
        
        return true;
    }

    /**
     * Determine whether Vera++ is enabled by the configuration
     * @param quiet set to true to suppress logging
     * @return true if Vera++ is enabled, false otherwise
     */
    protected boolean isVeraEnabled( boolean quiet ) 
    {
        if ( vera.skip() )
        {
            if ( ! quiet )
            {
                getLog().info( VeraConfiguration.SKIP_MESSAGE 
                        + ", 'skip' set to true in the " + VeraConfiguration.TOOL_NAME + " configuration." );
            }
            
            return false;
        }
        
        if ( vera.getVeraHome() == null ) 
        {
            if ( ! quiet )
            {
                getLog().info( VeraConfiguration.SKIP_MESSAGE 
                        + ", path to " + VeraConfiguration.TOOL_NAME + " home directory not set." );
            }
            
            return false;
        }        
        
        return true;
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
                getLog().info( CxxTestConfiguration.SKIP_MESSAGE + " " + stepName 
                        + ", 'skip' set to true in the " + CxxTestConfiguration.TOOL_NAME + " configuration." );
            }
            
            return false;
        }
        
        if ( cxxTest.getCxxTestHome() == null ) 
        {
            if ( ! quiet )
            {
                getLog().info( CxxTestConfiguration.SKIP_MESSAGE + " " + stepName 
                        + ", path to " + CxxTestConfiguration.TOOL_NAME + " not set." );
            }
            
            return false;
        }
        
        return true;
    }

    /**
     * Used by CxxTest Mojo classes to check the configuration of CxxTest
     * @throws MojoExecutionException if a problem is found with the configuration
     */
    protected void validateCxxTestConfiguration() throws MojoExecutionException 
    {
        final File cxxTestPythonHome = CxxTestGenMojo.getCxxTestPythonHome( cxxTest.getCxxTestHome() );
        
        if ( ! cxxTestPythonHome.isDirectory() )
        {
            throw new MojoExecutionException( "Could not find " + CxxTestConfiguration.TOOL_NAME 
                    + " Python scripts at " + cxxTestPythonHome, 
                    new FileNotFoundException( cxxTestPythonHome.getAbsolutePath() ) );
        }
        
        try 
        {
            MojoHelper.validateToolPath( new File( cxxTestPythonHome, "cxxtest/cxxtestgen.py" ), 
                    CxxTestConfiguration.TOOL_NAME, getLog() );
        }
        catch ( FileNotFoundException fnfe )
        {
            throw new MojoExecutionException( CxxTestConfiguration.TOOL_NAME + " home directory" 
                    + "could not be found at " + fnfe.getMessage() + ". "
                    + "You need to configure it in the plugin configuration section of the "
                    + "POM file using <cxxTestHome>...</cxxTestHome> or "
                    + "<properties><" + CxxTestConfiguration.HOME_PROPERTY 
                    + ">...</" + CxxTestConfiguration.HOME_PROPERTY + "></properties>; "
                    + "alternatively, you can use the command-line parameter -Dcxxtest.home=... "
                    + "or set the environment variable " + CxxTestConfiguration.HOME_ENVVAR, fnfe );
        }
        
        if ( cxxTest.getTestTargets() == null || cxxTest.getTestTargets().size() == 0 )
        {
            throw new MojoExecutionException( "You must specify at least one test target. If you want to skip "
                    + "running the tests, please set 'skip' to true in the " + CxxTestConfiguration.TOOL_NAME + " " 
                    + "configuration.", new InvalidParameterException( "testTargets" ) );
        }
        
        validateProjectFile();
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
     * Configure the Vera++ Mojo.
     */
    @Parameter
    protected VeraConfiguration vera = new VeraConfiguration();
    
    /**
     * Configure the Sonar Mojo.
     */
    @Parameter
    protected SonarConfiguration sonar = new SonarConfiguration();
    
    /**
     * The file extension for solution files.
     */
    private static final String SOLUTION_EXTENSION = "sln";

    /**
     * This handler capture standard Java logging produced by {@link VCProjectHolder} and relays it to the Maven logger
     * provided by the Mojo. It needs to be static to prevent duplicate log output. 
     * @see {@link LoggingHandler#LoggingHandler(String name)} 
     */
    private static final LoggingHandler VCPROJECT_HOLDER_LOG_HANDLER = 
            new LoggingHandler( VCProjectHolder.class.getName() );
    
}

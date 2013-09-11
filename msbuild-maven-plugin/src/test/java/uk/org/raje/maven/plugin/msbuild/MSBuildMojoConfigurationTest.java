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
import java.util.Arrays;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.junit.Test;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;

/**
 * Test MSBuildMojo configuration options.
 */
public class MSBuildMojoConfigurationTest extends AbstractMojoTestCase 
{

    @Test
    public final void testMissingPackagingConfiguration() throws Exception 
    {
        MSBuildMojo msbuildMojo = lookupMSBuildMojo( 
                "src/test/resources/unit/configurations/no-packaging-pom.xml" );
        try
        {
            msbuildMojo.execute();
            fail();
        }
        catch ( MojoExecutionException mee )
        {
            assertEquals( "Please set packaging to one of "
                    + "msbuild-solution, exe, dll, lib", mee.getMessage() );
        }
    }

    @Test
    public final void testMissingMSBuildConfiguration() throws Exception 
    {
        MSBuildMojo msbuildMojo = lookupMSBuildMojo( 
                "src/test/resources/unit/configurations/no-msbuild-pom.xml" );
        try
        {
            msbuildMojo.execute();
            fail();
        }
        catch ( MojoExecutionException mee )
        {
            assertEquals( "MSBuild could not be found. You need to configure it "
                    + "in the plugin configuration section in the pom file "
                    + "using <msbuild.path>...</msbuild.path> or "
                    + "<properties><msbuild.path>...</msbuild.path></properties> "
                    + "or on command-line using -Dmsbuild.path=... or by setting "
                    + "the environment variable MSBUILD_PATH", mee.getMessage() );
        }
    }

    @Test
    public final void testMissingProjectConfiguration() throws Exception 
    {
        MSBuildMojo msbuildMojo = lookupMSBuildMojo( 
                "src/test/resources/unit/configurations/missing-project-pom.xml" );
        try
        {
            msbuildMojo.execute();
            fail();
        }
        catch ( MojoExecutionException mee )
        {
            assertEquals( "Missing projectFile, please check your configuration",
                    mee.getMessage() );
        }
    }

    @Test
    public final void testMinimalSolutionConfiguration() throws Exception
    {
        MSBuildMojo msbuildMojo = lookupMSBuildMojo( 
                "src/test/resources/unit/configurations/minimal-solution-pom.xml" );
        
        assertNull( msbuildMojo.platforms );
        msbuildMojo.execute();
        assertEquals( Arrays.asList( new BuildPlatform( "Win32" ) ), msbuildMojo.platforms );
        assertEquals( Arrays.asList( new BuildConfiguration( "Release" ) ), 
                msbuildMojo.platforms.get( 0 ).getConfigurations() );
    }

    @Test
    public final void testMinimalProjectConfiguration() throws Exception
    {
        MSBuildMojo msbuildMojo = lookupMSBuildMojo( 
                "src/test/resources/unit/configurations/minimal-project-pom.xml" );
        
        assertNull( msbuildMojo.platforms );
        msbuildMojo.execute();
        assertEquals( Arrays.asList( new BuildPlatform( "Win32" ) ), msbuildMojo.platforms );
        assertEquals( Arrays.asList( new BuildConfiguration( "Release" ) ), 
                msbuildMojo.platforms.get( 0 ).getConfigurations() );
    }

    /**
     * Note: This test doesn't execute the Mojo, just test configuration 
     */
    @Test
    public final void testPlatformsConfiguration() throws Exception
    {
        MSBuildMojo msbuildMojo = lookupMSBuildMojo( 
                "src/test/resources/unit/configurations/platforms-pom.xml" );

        assertEquals( 
                Arrays.asList( new BuildPlatform( "Win32" ), new BuildPlatform( "x64" ) ),
                msbuildMojo.platforms );
        assertEquals( Arrays.asList( new BuildConfiguration( "Release" ) ), 
                msbuildMojo.platforms.get( 0 ).getConfigurations() );
        assertEquals( Arrays.asList( new BuildConfiguration( "Release" ) ), 
                msbuildMojo.platforms.get( 1 ).getConfigurations() );
    }

    /**
     * Note: This test doesn't execute the Mojo, just test configuration 
     */
    @Test
    public final void testConfigurationsConfiguration() throws Exception
    {
        MSBuildMojo msbuildMojo = lookupMSBuildMojo( 
                "src/test/resources/unit/configurations/configurations-pom.xml" );

        assertEquals( Arrays.asList( new BuildPlatform( "Win32" ), new BuildPlatform( "x64" ) ),
                msbuildMojo.platforms );
        assertEquals( Arrays.asList( new BuildConfiguration( "Release" ), new BuildConfiguration( "Debug" ) ), 
                msbuildMojo.platforms.get( 0 ).getConfigurations() );
        assertEquals( Arrays.asList( new BuildConfiguration( "Release" ) ), 
                msbuildMojo.platforms.get( 1 ).getConfigurations() );

    }

    /**
     * Workaround for parent class lookupMojo and lookupConfiguredMojo.
     * @param pomPath where to find the POM file
     * @return a configured MSBuild Mojo for testing
     * @throws Exception if we can't find the Mojo or the POM is malformed
     */
    protected final MSBuildMojo lookupMSBuildMojo( String pomPath ) throws Exception
    {
        File pom = getTestFile( pomPath );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        // The following 4 lines are simply to get a MavenProject object
        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        ProjectBuildingRequest buildingRequest = executionRequest.getProjectBuildingRequest();
        ProjectBuilder projectBuilder = this.lookup( ProjectBuilder.class );
        MavenProject mavenProject = projectBuilder.build( pom, buildingRequest ).getProject();
        assertNotNull( mavenProject );
        
        // Used lookupMojo as it sets up most of what we need and reads configuration
        // variables from the poms.
        // It doesn't set a MavenProject so we have to do that manually
        // lookupConfiguredMojo doesn't work properly, configuration variables are no expanded
        // as we expect and it fails to setup a Log.
        MSBuildMojo msbuildMojo = (MSBuildMojo) lookupMojo( MSBuildMojo.MOJO_NAME, pom );
        assertNotNull( msbuildMojo );
        msbuildMojo.mavenProject = mavenProject;

        return msbuildMojo;
    }
}

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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;

/**
 * Test MSBuildMojo configuration options.
 */
public class MSBuildMojoConfigurationTest extends AbstractMSBuildMojoTestCase 
{
    @Override
    public final void setUp() throws Exception
    {
        super.setUp();
        outputStream = new ByteArrayOutputStream();
        stdout = System.out;
        System.setOut( new PrintStream( outputStream ) );
    }

    @Override
    public final void tearDown() throws Exception
    {
        super.tearDown();
        System.setOut( stdout );
        outputStream.close();
        outputStream = null;
    }

    @Test
    public final void testAllSettingsConfiguration() throws Exception 
    {
        MSBuildMojo msbuildMojo = ( MSBuildMojo ) lookupConfiguredMojo( MSBuildMojo.MOJO_NAME, 
                "/unit/configurations/allsettings-pom.xml" );

        assertAllSettingsConfiguration( msbuildMojo );
    }

    @Test
    public final void testMissingPackagingConfiguration() throws Exception 
    {
        MSBuildMojo msbuildMojo = ( MSBuildMojo ) lookupConfiguredMojo( MSBuildMojo.MOJO_NAME, 
                "/unit/configurations/no-packaging-pom.xml" );
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
        MSBuildMojo msbuildMojo = ( MSBuildMojo ) lookupConfiguredMojo( MSBuildMojo.MOJO_NAME, 
                "/unit/configurations/no-msbuild-pom.xml" );
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
        MSBuildMojo msbuildMojo = ( MSBuildMojo ) lookupConfiguredMojo( MSBuildMojo.MOJO_NAME, 
                "/unit/configurations/missing-project-pom.xml" );
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
        MSBuildMojo msbuildMojo = ( MSBuildMojo ) lookupConfiguredMojo( MSBuildMojo.MOJO_NAME, 
                "/unit/configurations/minimal-solution-pom.xml" );
        
        assertNull( msbuildMojo.platforms );
        msbuildMojo.execute();
        assertEquals( Arrays.asList( new BuildPlatform( "Win32" ) ), msbuildMojo.platforms );
        assertEquals( Arrays.asList( new BuildConfiguration( "Release" ) ), 
                msbuildMojo.platforms.get( 0 ).getConfigurations() );
    }

    @Test
    public final void testMinimalProjectConfiguration() throws Exception
    {
        MSBuildMojo msbuildMojo = ( MSBuildMojo ) lookupConfiguredMojo( MSBuildMojo.MOJO_NAME, 
                "/unit/configurations/minimal-project-pom.xml" );
        
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
        MSBuildMojo msbuildMojo = ( MSBuildMojo ) lookupConfiguredMojo( MSBuildMojo.MOJO_NAME, 
                "/unit/configurations/platforms-pom.xml" );

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
        MSBuildMojo msbuildMojo = ( MSBuildMojo ) lookupConfiguredMojo( MSBuildMojo.MOJO_NAME, 
                "/unit/configurations/configurations-pom.xml" );

        assertEquals( Arrays.asList( new BuildPlatform( "Win32" ), new BuildPlatform( "x64" ) ),
                msbuildMojo.platforms );
        assertEquals( Arrays.asList( new BuildConfiguration( "Release" ), new BuildConfiguration( "Debug" ) ), 
                msbuildMojo.platforms.get( 0 ).getConfigurations() );
        assertEquals( Arrays.asList( new BuildConfiguration( "Release" ) ), 
                msbuildMojo.platforms.get( 1 ).getConfigurations() );

    }

    @Test
    public final void testDefaultMaxCpuCountConfiguration() throws Exception
    {
        MSBuildMojo msbuildMojo = ( MSBuildMojo ) lookupConfiguredMojo( MSBuildMojo.MOJO_NAME, 
                "/unit/configurations/minimal-solution-pom.xml" );
        
        msbuildMojo.execute();
        
        assertTrue( "MSBuild command line error /maxcpucount not found",
                outputStream.toString().contains( "/maxcpucount " ) );
    }

    @Test
    public final void testMaxCpuCount4Configuration() throws Exception
    {
        MSBuildMojo msbuildMojo = ( MSBuildMojo ) lookupConfiguredMojo( MSBuildMojo.MOJO_NAME, 
                "/unit/configurations/minimal-solution-with-maxcpucount-pom.xml" );
        
        msbuildMojo.execute();
        
        assertTrue( "MSBuild command line error /maxcpucount:4 not found",
                outputStream.toString().contains( "/maxcpucount:4 " ) );
    }

    private PrintStream stdout;
    private ByteArrayOutputStream outputStream;
}

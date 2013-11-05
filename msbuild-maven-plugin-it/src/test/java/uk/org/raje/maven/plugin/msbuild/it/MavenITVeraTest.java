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

package uk.org.raje.maven.plugin.msbuild.it;

import static org.junit.Assert.fail;
import static uk.org.raje.maven.plugin.msbuild.it.MSBuildMojoITHelper.addPropertiesToVerifier;

import java.io.File;

import junitx.framework.FileAssert;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Test;

import uk.org.raje.maven.plugin.msbuild.VeraMojo;

/**
 * Integration test that runs Vera++ on a test project
 */
public class MavenITVeraTest 
{
    @Test
    public void testEnvVarPathConfiguration() throws Exception
    {
        final File testDir = new File( getClass().getResource( "/compute-pi-vera-test" ).getPath() );
        final Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        addPropertiesToVerifier( verifier );
        verifier.setEnvironmentVariable( VERA_HOME_ENVVAR, VERA_FAKE_HOME );
        verifier.setMavenDebug( true );

        try
        {
            verifier.executeGoal( GROUPID + ":" + ARTIFACTID + ":" + VeraMojo.MOJO_NAME );
            fail( "Expected execution failure due to invalid home directory" );
        }
        catch ( VerificationException ve )
        {
            verifier.verifyTextInLog( VERA_FAKE_HOME );
        }
    }

    @Test
    public void testVeraChecksOnSolution() throws Exception
    {
        final File testDir = new File( getClass().getResource( "/compute-pi-vera-test" ).getPath() );
        final Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        addPropertiesToVerifier( verifier );
        verifier.getSystemProperties().setProperty( MSBuildMojoITHelper.MSBUILD_PLUGIN_TOOLS_ENABLE, "true" );
        verifier.setMavenDebug( true );
        
        verifier.executeGoal( GROUPID + ":" + ARTIFACTID + ":" + VeraMojo.MOJO_NAME );
        verifier.verifyErrorFreeLog();
        
        FileAssert.assertEquals( 
                new File( testDir, "expected/vera-report-compute-pi-Win32-Debug.xml" ), 
                new File( testDir, "compute-pi/checkstyle-reports/vera-report-compute-pi-Win32-Debug.xml" ) );

        FileAssert.assertEquals( 
                new File( testDir, "expected/vera-report-compute-pi-Win32-Release.xml" ), 
                new File( testDir, "compute-pi/checkstyle-reports/vera-report-compute-pi-Win32-Release.xml" ) );

        FileAssert.assertEquals( 
                new File( testDir, "expected/vera-report-compute-pi-test-Win32-Debug.xml" ), 
                new File( testDir, "compute-pi-test/checkstyle-reports/vera-report-compute-pi-test-Win32-Debug.xml" ) );

        FileAssert.assertEquals( 
                new File( testDir, "expected/vera-report-compute-pi-test-Win32-Release.xml" ), 
                new File( testDir, 
                        "compute-pi-test/checkstyle-reports/vera-report-compute-pi-test-Win32-Release.xml" ) );
    }

    @Test
    public void testVeraChecksOnProject() throws Exception
    {
        final File testDir = new File( getClass().getResource( "/compute-pi-vera-test/compute-pi" ).getPath() );
        final Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        
        addPropertiesToVerifier( verifier );
        verifier.getSystemProperties().setProperty( MSBuildMojoITHelper.MSBUILD_PLUGIN_TOOLS_ENABLE, "true" );
        verifier.setMavenDebug( true );
        
        verifier.executeGoal( GROUPID + ":" + ARTIFACTID + ":" + VeraMojo.MOJO_NAME );
        verifier.verifyErrorFreeLog();
        
        FileAssert.assertEquals( 
                new File( testDir, "expected/vera-report-compute-pi-Win32-Debug.xml" ), 
                new File( testDir, "checkstyle-reports/vera-report-compute-pi-Win32-Debug.xml" ) );

        FileAssert.assertEquals( 
                new File( testDir, "expected/vera-report-compute-pi-Win32-Release.xml" ), 
                new File( testDir, "checkstyle-reports/vera-report-compute-pi-Win32-Release.xml" ) );
    }

    private static final String GROUPID = "uk.org.raje.maven.plugins";
    private static final String ARTIFACTID = "msbuild-maven-plugin";
    private static final String VERA_HOME_ENVVAR = "VERA_HOME";
    private static final String VERA_FAKE_HOME = "fake-home";
}

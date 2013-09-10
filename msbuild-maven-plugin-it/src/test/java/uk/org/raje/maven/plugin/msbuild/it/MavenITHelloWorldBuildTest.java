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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Test;

import static uk.org.raje.maven.plugin.msbuild.it.MSBuildMojoITHelper.addPropertiesToVerifier;
import static uk.org.raje.maven.plugin.msbuild.it.MSBuildMojoITHelper.assertDirectoryContents;
import static uk.org.raje.maven.plugin.msbuild.it.MSBuildMojoITHelper.calculateAndDeleteOutputDirectory;
import static uk.org.raje.maven.plugin.msbuild.it.MSBuildMojoITHelper.checkProjectBuildOutputIsCleaned;

/**
 * Integration test that runs the hello-world-build-test
 *
 */
public class MavenITHelloWorldBuildTest 
{

    @Test
    public void testSolutionBuild() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/hello-world-build-test" );
        File releaseDir = calculateAndDeleteOutputDirectory( testDir, "Release" );
        File debugDir = calculateAndDeleteOutputDirectory( testDir, "Debug" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        addPropertiesToVerifier( verifier );
        
        // Delete any existing artifact from the local repository
        verifier.deleteArtifacts( GROUPID, SOLUTION_ARTIFACTID, VERSION );

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        assertDirectoryContents( releaseDir, 2, Arrays.asList( 
                new String[]{"hello-world.exe", "hello-world.pdb"} ) );
        assertDirectoryContents( debugDir, 3, Arrays.asList( 
                new String[]{"hello-world.exe", "hello-world.ilk", "hello-world.pdb"} ) );
        
        File artifactsDir = new File( 
                verifier.getArtifactMetadataPath( GROUPID, SOLUTION_ARTIFACTID, VERSION ) ).getParentFile();
        assertDirectoryContents( artifactsDir, 5, Arrays.asList( new String[]{
                SOLUTION_ARTIFACTID + "-" + VERSION + ".pom",
                SOLUTION_ARTIFACTID + "-" + VERSION + "-Win32-Release.zip",
                SOLUTION_ARTIFACTID + "-" + VERSION + "-Win32-Debug.zip"} ) );

        verifier.resetStreams();
        
        verifier.executeGoal( "clean" );
        verifier.verifyErrorFreeLog();
        assertEquals( 0, releaseDir.list().length );
        assertEquals( 0, debugDir.list().length );
    }

    @Test
    public void testSolutionClean() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/hello-world-build-test" );
        File releaseDir = new File( testDir, "Release" );
        File debugDir = new File( testDir, "Debug" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        addPropertiesToVerifier( verifier );
        
        verifier.executeGoal( "clean" );
        verifier.verifyErrorFreeLog();
        if ( releaseDir.exists() )
        {
            assertEquals( 0, releaseDir.list().length );
        }
        if ( debugDir.exists() )
        {
            assertEquals( 0, debugDir.list().length );
        }
    }

    @Test
    public void testProjectBuild() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/hello-world-build-test/hello-world" );
        File releaseDir = calculateAndDeleteOutputDirectory( testDir, "Release" );
        File debugDir = calculateAndDeleteOutputDirectory( testDir, "Debug" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        addPropertiesToVerifier( verifier );
        
        // Delete any existing artifact from the local repository
        verifier.deleteArtifacts( GROUPID, PROJECT_ARTIFACTID, VERSION );

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        // We don't check all 16 files, just the most important ones
        assertDirectoryContents( releaseDir, HELLOWORLD_PROJECT_RELEASE_FILE_COUNT, Arrays.asList( 
                new String[]{"hello-world.exe", "hello-world.pdb"} ) );
        // We don't check all 30 files, just the most important ones
        assertDirectoryContents( debugDir, HELLOWORLD_PROJECT_DEBUG_FILE_COUNT, Arrays.asList( 
                new String[]{"hello-world.exe", "hello-world.ilk", "hello-world.pdb"} ) );

        File artifactsDir = new File( 
                verifier.getArtifactMetadataPath( GROUPID, PROJECT_ARTIFACTID, VERSION ) ).getParentFile();
        assertDirectoryContents( artifactsDir, 5, Arrays.asList( new String[]{
                PROJECT_ARTIFACTID + "-" + VERSION + ".pom",
                PROJECT_ARTIFACTID + "-" + VERSION + ".exe",
                PROJECT_ARTIFACTID + "-" + VERSION + "-Win32-Debug.exe"} ) );

        verifier.resetStreams();
        
        verifier.executeGoal( "clean" );
        verifier.verifyErrorFreeLog();
        checkProjectBuildOutputIsCleaned( "hello-world", releaseDir );
        checkProjectBuildOutputIsCleaned( "hello-world", debugDir );
    }

    @Test
    public void testProjectClean() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/hello-world-build-test/hello-world" );
        File releaseDir = new File( testDir, "Release" );
        File debugDir = new File( testDir, "Debug" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        addPropertiesToVerifier( verifier );
        
        verifier.executeGoal( "clean" );
        verifier.verifyErrorFreeLog();
        checkProjectBuildOutputIsCleaned( "hello-world", releaseDir );
        checkProjectBuildOutputIsCleaned( "hello-world", debugDir );
    }


    private static final String GROUPID = "uk.org.raje.maven.plugins.msbuild.it";
    private static final String SOLUTION_ARTIFACTID = "hello-world-build-solution-test";
    private static final String PROJECT_ARTIFACTID = "hello-world-build-project-test";
    private static final String VERSION = "1-SNAPSHOT";

    private static final int HELLOWORLD_PROJECT_RELEASE_FILE_COUNT = 15;
    private static final int HELLOWORLD_PROJECT_DEBUG_FILE_COUNT = 29;
}

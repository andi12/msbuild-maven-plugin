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

/**
 * Integration test that runs the multi-platform-test
 *
 */
public class MavenITMultiPlatformTest 
{

    @Test
    public void testSolutionBuild() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/multi-platform-test" );
        File win32ReleaseDir = calculateAndDeleteOutputDirectory( testDir, "Release" );
        File win32DebugDir = calculateAndDeleteOutputDirectory( testDir, "Debug" );
        File x64ReleaseDir = calculateAndDeleteOutputDirectory( testDir, "x64\\Release" );
        File x64DebugDir = calculateAndDeleteOutputDirectory( testDir, "x64\\Debug" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        addPropertiesToVerifier( verifier );
        
        // Delete any existing artifact from the local repository
        verifier.deleteArtifacts( GROUPID, SOLUTION_ARTIFACTID, VERSION );

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        assertDirectoryContents( win32ReleaseDir, 2, Arrays.asList( 
                new String[]{"multi-platform-test.exe", "multi-platform-test.pdb"} ) );
        assertDirectoryContents( win32DebugDir, 3, Arrays.asList( 
                new String[]{"multi-platform-test.exe", "multi-platform-test.ilk", "multi-platform-test.pdb"} ) );
        assertDirectoryContents( x64ReleaseDir, 2, Arrays.asList( 
                new String[]{"multi-platform-test.exe", "multi-platform-test.pdb"} ) );
        assertDirectoryContents( x64DebugDir, 3, Arrays.asList( 
                new String[]{"multi-platform-test.exe", "multi-platform-test.ilk", "multi-platform-test.pdb"} ) );
        
        File artifactsDir = new File( 
                verifier.getArtifactMetadataPath( GROUPID, SOLUTION_ARTIFACTID, VERSION ) ).getParentFile();
        assertDirectoryContents( artifactsDir, 7, Arrays.asList( new String[]{
                SOLUTION_ARTIFACTID + "-" + VERSION + ".pom",
                SOLUTION_ARTIFACTID + "-" + VERSION + "-Win32-Release.zip",
                SOLUTION_ARTIFACTID + "-" + VERSION + "-Win32-Debug.zip",
                SOLUTION_ARTIFACTID + "-" + VERSION + "-x64-Release.zip",
                SOLUTION_ARTIFACTID + "-" + VERSION + "-x64-Debug.zip"} ) );

        verifier.resetStreams();
        
        verifier.executeGoal( "clean" );
        verifier.verifyErrorFreeLog();
        assertEquals( 0, win32ReleaseDir.list().length );
        assertEquals( 0, win32DebugDir.list().length );
    }

    private static final String GROUPID = "uk.org.raje.maven.plugins.msbuild.it";
    private static final String SOLUTION_ARTIFACTID = "multi-platform-solution-test";
    private static final String PROJECT_ARTIFACTID = "multi-platform-project-test";
    private static final String VERSION = "1-SNAPSHOT";
}

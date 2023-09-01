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
public class MavenITOutputDirectoryTest
{

    @Test
    public void testSolutionBuild() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/output-directory-test" );
        File win32ReleaseDir = calculateAndDeleteOutputDirectory( testDir, "Runtime\\Release" );
        File win32DebugDir = calculateAndDeleteOutputDirectory( testDir, "Runtime\\Debug" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        addPropertiesToVerifier( verifier );

        // Delete any existing artifact from the local repository
        verifier.deleteArtifacts( GROUPID, SOLUTION_ARTIFACTID, VERSION );

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        assertDirectoryContents( win32ReleaseDir, 5, Arrays.asList( new String[]
                {"output-directory-test.exe", "output-directory-test.iobj", "output-directory-test-lib.lib",
                 "output-directory-test.ipdb", "output-directory-test.pdb"} ) );
        assertDirectoryContents( win32DebugDir, 5, Arrays.asList( new String[]
                {"output-directory-test.exe", "output-directory-test.ilk", "output-directory-test.pdb",
                 "output-directory-test-lib.lib", "output-directory-test-lib.pdb"} ) );

        File artifactsDir = new File(
                verifier.getArtifactMetadataPath( GROUPID, SOLUTION_ARTIFACTID, VERSION ) ).getParentFile();
        assertDirectoryContents( artifactsDir, 5, Arrays.asList( new String[]{
                SOLUTION_ARTIFACTID + "-" + VERSION + ".pom",
                SOLUTION_ARTIFACTID + "-" + VERSION + "-Win32-Release.zip",
                SOLUTION_ARTIFACTID + "-" + VERSION + "-Win32-Debug.zip"} ) );

        verifier.resetStreams();

        verifier.executeGoal( "clean" );
        verifier.verifyErrorFreeLog();
        assertEquals( 0, win32ReleaseDir.list().length );
        assertEquals( 0, win32DebugDir.list().length );
    }

    @Test
    public void testLibProjectBuild() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/output-directory-test/output-directory-test-lib" );
        File win32ReleaseDir = calculateAndDeleteOutputDirectory( testDir, "Runtime\\Release" );
        File win32DebugDir = calculateAndDeleteOutputDirectory( testDir, "Runtime\\Debug" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        addPropertiesToVerifier( verifier );

        // Delete any existing artifact from the local repository
        verifier.deleteArtifacts( GROUPID, SOLUTION_ARTIFACTID, VERSION );

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        assertDirectoryContents( win32ReleaseDir, 1, Arrays.asList( new String[]
                {"output-directory-test-lib.lib"} ) );
        assertDirectoryContents( win32DebugDir, 2, Arrays.asList( new String[]
                {"output-directory-test-lib.lib", "output-directory-test-lib.pdb"} ) );

        File artifactsDir = new File(
                verifier.getArtifactMetadataPath( GROUPID, LIBPROJ_ARTIFACTID, VERSION ) ).getParentFile();
        assertDirectoryContents( artifactsDir, 6, Arrays.asList( new String[]{
                LIBPROJ_ARTIFACTID + "-" + VERSION + ".pom",
                LIBPROJ_ARTIFACTID + "-" + VERSION + ".lib",
                LIBPROJ_ARTIFACTID + "-" + VERSION + "-Win32-Debug.lib"} ) );

        verifier.resetStreams();

        verifier.executeGoal( "clean" );
        verifier.verifyErrorFreeLog();
        assertEquals( 0, win32ReleaseDir.list().length );
        assertEquals( 0, win32DebugDir.list().length );
    }

    private static final String GROUPID = "uk.org.raje.maven.plugins.msbuild.it";
    private static final String SOLUTION_ARTIFACTID = "output-directory-solution-test";
    private static final String LIBPROJ_ARTIFACTID = "output-directory-libproj-test";
    private static final String VERSION = "1-SNAPSHOT";
}

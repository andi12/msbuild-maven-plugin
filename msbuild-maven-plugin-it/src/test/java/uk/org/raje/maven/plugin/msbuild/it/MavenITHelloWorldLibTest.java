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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Test;

import uk.org.raje.maven.plugin.msbuild.MSBuildPackaging;
import static uk.org.raje.maven.plugin.msbuild.it.MSBuildMojoITHelper.calculateAndDeleteOutputDirectory;
import static uk.org.raje.maven.plugin.msbuild.it.MSBuildMojoITHelper.checkProjectBuildOutputIsCleaned;

/**
 * Integration test that runs the hello-world-lib-test
 *
 */
public class MavenITHelloWorldLibTest 
{

    @Test
    public void testSolutionBuild() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/hello-world-lib-test" );
        File releaseDir = calculateAndDeleteOutputDirectory( testDir, "Release" );
        File debugDir = calculateAndDeleteOutputDirectory( testDir, "Debug" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        
        // Delete any existing artifact from the local repository
        verifier.deleteArtifact( GROUPID, SOLUTION_ARTIFACTID, VERSION, "lib" );

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        List<String> releaseDirContents = Arrays.asList( releaseDir.list() );
        assertEquals( 1, releaseDirContents.size() );
        assertTrue( releaseDirContents.contains( "hello-world-lib.lib" ) ); 
        List<String> debugDirContents = Arrays.asList( debugDir.list() );
        assertEquals( 1, debugDirContents.size() );
        assertTrue( debugDirContents.contains( "hello-world-lib.lib" ) ); 
        
        verifier.resetStreams();
        
        verifier.executeGoal( "clean" );
        verifier.verifyErrorFreeLog();
        assertEquals( 0, releaseDir.list().length );
        assertEquals( 0, debugDir.list().length );
    }

    @Test
    public void testProjectBuild() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/hello-world-lib-test/hello-world-lib" );
        File releaseDir = calculateAndDeleteOutputDirectory( testDir, "Release" );
        File debugDir = calculateAndDeleteOutputDirectory( testDir, "Debug" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        
        // Delete any existing artifact from the local repository
        verifier.deleteArtifact( GROUPID, PROJECT_ARTIFACTID, VERSION, "lib" );

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        List<String> releaseDirContents = Arrays.asList( releaseDir.list() );
        assertEquals( HELLOWORLD_PROJECT_RELEASE_FILE_COUNT, releaseDirContents.size() );
        // We don't check all 10 files, just the most important ones
        assertTrue( releaseDirContents.contains( "hello-world-lib.lib" ) ); 
        List<String> debugDirContents = Arrays.asList( debugDir.list() );
        assertEquals( HELLOWORLD_PROJECT_DEBUG_FILE_COUNT, debugDirContents.size() );
        // We don't check all 11 files, just the most important ones
        assertTrue( debugDirContents.contains( "hello-world-lib.lib" ) ); 
        
        verifier.assertArtifactPresent( GROUPID,
                PROJECT_ARTIFACTID,
                VERSION, 
                MSBuildPackaging.LIB );
        // NOTE: verifier doesn't appear to provide a way to check artifacts with other classifiers

        verifier.resetStreams();
        
        verifier.executeGoal( "clean" );
        verifier.verifyErrorFreeLog();
        checkProjectBuildOutputIsCleaned( "hello-world-lib", releaseDir );
        checkProjectBuildOutputIsCleaned( "hello-world-lib", debugDir );
    }

    private static final String GROUPID = "uk.org.raje.maven.plugins.msbuild.it";
    private static final String SOLUTION_ARTIFACTID = "hello-world-lib-solution-test";
    private static final String PROJECT_ARTIFACTID = "hello-world-lib-project-test";
    private static final String VERSION = "1-SNAPSHOT";

    private static final int HELLOWORLD_PROJECT_RELEASE_FILE_COUNT = 10;
    private static final int HELLOWORLD_PROJECT_DEBUG_FILE_COUNT = 11;
}

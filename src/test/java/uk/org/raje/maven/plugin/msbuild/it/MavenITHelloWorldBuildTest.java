/*
 * Copyright 2013 Andrew Everitt, Andrew Heckford, Daniele Daniele
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

/**
 * Integration test that runs the hello-world-build-test
 *
 */
public class MavenITHelloWorldBuildTest 
{

    @Test
    public void testBuild() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/it/hello-world-build-test" );
        File releaseDir = new File( testDir, "Release" );
        File debugDir = new File( testDir, "Debug" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        
        // Delete any existing artifact from the local repository
        verifier.deleteArtifact( "uk.org.raje.maven.plugins.msbuild.it", "hello-world-build-test",
                "1-SNAPSHOT", "exe" );

        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        List<String> releaseDirContents = Arrays.asList( releaseDir.list() );
        assertEquals( 2, releaseDirContents.size() );
        assertTrue( releaseDirContents.contains( "hello-world.exe" ) ); 
        assertTrue( releaseDirContents.contains( "hello-world.pdb" ) ); 
        List<String> debugDirContents = Arrays.asList( debugDir.list() );
        assertEquals( 3, debugDirContents.size() );
        assertTrue( debugDirContents.contains( "hello-world.exe" ) ); 
        assertTrue( debugDirContents.contains( "hello-world.ilk" ) ); 
        assertTrue( debugDirContents.contains( "hello-world.pdb" ) ); 
        
        verifier.resetStreams();
        
        verifier.executeGoal( "clean" );
        verifier.verifyErrorFreeLog();
        assertEquals( 0, releaseDir.list().length );
        assertEquals( 0, debugDir.list().length );
    }

}

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

import static uk.org.raje.maven.plugin.msbuild.it.MSBuildMojoITHelper.addPropertiesToVerifier;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Test;

import uk.org.raje.maven.plugin.msbuild.CppCheckMojo;

/**
 * Integration test that runs the hello-world-build-test
 *
 */
public class MavenITHelloWorldCppCheckTest 
{

    @Test
    public void testSolutionBuild() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/hello-world-cppcheck-test" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        addPropertiesToVerifier( verifier );
        
        verifier.executeGoal( GROUPID + ":" + ARTIFACTID + ":" + CppCheckMojo.MOJO_NAME );
        verifier.verifyErrorFreeLog();
    }

    private static final String GROUPID = "uk.org.raje.maven.plugins";
    private static final String ARTIFACTID = "msbuild-maven-plugin";
}

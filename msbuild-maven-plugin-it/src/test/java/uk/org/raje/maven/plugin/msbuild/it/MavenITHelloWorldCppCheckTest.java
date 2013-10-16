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
    public void envVarPathConfiguration() throws Exception
    {
        final File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/hello-world-cppcheck-test" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        addPropertiesToVerifier( verifier );
        // As we can't know a real location put a fake one and check the error message
        verifier.setEnvironmentVariable( "CPPCHECK_PATH", "my_fake_path" );

        try
        {
            verifier.executeGoal( GROUPID + ":" + ARTIFACTID + ":" + CppCheckMojo.MOJO_NAME );
            fail();
        }
        catch ( VerificationException ve )
        {
            verifier.verifyTextInLog( "my_fake_path" );
        }
    }

    @Test
    public void solutionCheck() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/hello-world-cppcheck-test" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        addPropertiesToVerifier( verifier );
        verifier.getSystemProperties().setProperty( MSBuildMojoITHelper.MSBUILD_PLUGIN_TOOLS_ENABLE, "true" );
        
        verifier.executeGoal( GROUPID + ":" + ARTIFACTID + ":" + CppCheckMojo.MOJO_NAME );
        verifier.verifyErrorFreeLog();
        
        FileAssert.assertEquals( 
                new File( testDir, "expected\\cppcheck-report-hello-world-Win32-Debug.xml" ), 
                new File( testDir, "hello-world\\cppcheck-reports\\cppcheck-report-hello-world-Win32-Debug.xml" ) );

        FileAssert.assertEquals( 
                new File( testDir, "expected\\cppcheck-report-hello-world-Win32-Release.xml" ), 
                new File( testDir, "hello-world\\cppcheck-reports\\cppcheck-report-hello-world-Win32-Release.xml" ) );
    }

    @Test
    public void projectCheck() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/hello-world-cppcheck-test/hello-world" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        addPropertiesToVerifier( verifier );
        verifier.getSystemProperties().setProperty( MSBuildMojoITHelper.MSBUILD_PLUGIN_TOOLS_ENABLE, "true" );
        
        verifier.executeGoal( GROUPID + ":" + ARTIFACTID + ":" + CppCheckMojo.MOJO_NAME );
        verifier.verifyErrorFreeLog();
        
        FileAssert.assertEquals( 
                new File( testDir, "expected\\cppcheck-report-hello-world-Win32-Debug.xml" ), 
                new File( testDir, "cppcheck-reports\\cppcheck-report-hello-world-Win32-Debug.xml" ) );

        FileAssert.assertEquals( 
                new File( testDir, "expected\\cppcheck-report-hello-world-Win32-Release.xml" ), 
                new File( testDir, "cppcheck-reports\\cppcheck-report-hello-world-Win32-Release.xml" ) );
    }

    private static final String GROUPID = "uk.org.raje.maven.plugins";
    private static final String ARTIFACTID = "msbuild-maven-plugin";
}

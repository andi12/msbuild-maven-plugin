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
import static uk.org.raje.maven.plugin.msbuild.it.MSBuildMojoITHelper.addPropertiesToVerifier;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import junitx.framework.FileAssert;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Test;

import uk.org.raje.maven.plugin.msbuild.CxxTestBuildMojo;
import uk.org.raje.maven.plugin.msbuild.CxxTestGenMojo;
import uk.org.raje.maven.plugin.msbuild.CxxTestRunnerMojo;

/**
 * Integration test that runs the hello-world-build-test
 *
 */
public class MavenITCxxTestMojoTest 
{
    @Test
    public void testCxxTestGenerateAndBuild() throws Exception
    {
        final File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/compute-pi-cxxtest-test" );
        final File outputFile = calculateAndDeleteTestRunnerCpp( "/compute-pi-cxxtest-test/compute-pi-test/" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        addPropertiesToVerifier( verifier );
        verifier.getSystemProperties().setProperty( MSBuildMojoITHelper.MSBUILD_PLUGIN_TOOLS_ENABLE, "true" );
        
        verifier.executeGoal( GROUPID + ":" + ARTIFACTID + ":" + CxxTestGenMojo.MOJO_NAME );
        verifier.verifyErrorFreeLog();
        assertTrue( "Test runner not generated", outputFile.exists() );
        verifier.resetStreams();

        FileAssert.assertEquals( 
                createExpected( new File( testDir, "compute-pi-test" ), 
                        new File( testDir, "expected\\cxxtest-runner.cpp" ) ), 
                new File( testDir, "compute-pi-test\\cxxtest-runner.cpp" ) );

        verifier.executeGoal( GROUPID + ":" + ARTIFACTID + ":" + CxxTestBuildMojo.MOJO_NAME );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        verifier.executeGoal( GROUPID + ":" + ARTIFACTID + ":" + CxxTestRunnerMojo.MOJO_NAME );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        FileAssert.assertEquals( 
                new File( testDir, "expected\\cxxtest-report-compute-pi-test-Win32-Debug.xml" ),
                new File( testDir, "target\\surefire-reports\\cxxtest-report-compute-pi-test-Win32-Debug.xml" ) );
        FileAssert.assertEquals( 
                new File( testDir, "expected\\cxxtest-report-compute-pi-test-Win32-Release.xml" ),
                new File( testDir, "target\\surefire-reports\\cxxtest-report-compute-pi-test-Win32-Release.xml" ) );
    }
    
    @Test
    public void testCxxTestGenerateNoUpdate() throws Exception
    {
        final File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/compute-pi-cxxtest-test" );
        final File outputFile = calculateAndDeleteTestRunnerCpp( "/compute-pi-cxxtest-test/compute-pi-test/" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        addPropertiesToVerifier( verifier );
        verifier.getSystemProperties().setProperty( MSBuildMojoITHelper.MSBUILD_PLUGIN_TOOLS_ENABLE, "true" );
        
        verifier.executeGoal( GROUPID + ":" + ARTIFACTID + ":" + CxxTestGenMojo.MOJO_NAME );
        verifier.verifyErrorFreeLog();
        assertTrue( "Test runner not generated", outputFile.exists() );
        long lastUpdate = outputFile.lastModified();
        verifier.resetStreams();

        verifier.executeGoal( GROUPID + ":" + ARTIFACTID + ":" + CxxTestGenMojo.MOJO_NAME );
        verifier.verifyErrorFreeLog();
        assertTrue( "Test runner not generated", outputFile.exists() );
        assertEquals( lastUpdate, outputFile.lastModified() );
        verifier.resetStreams();
    }
    
    @Test
    public void testCxxTestTemplateGenerateAndBuild() throws Exception
    {
        final File testDir = ResourceExtractor.simpleExtractResources( getClass(), 
                "/compute-pi-cxxtest-template-test" );
        final File outputFile = calculateAndDeleteTestRunnerCpp( "/compute-pi-cxxtest-template-test/compute-pi-test/" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        addPropertiesToVerifier( verifier );
        verifier.getSystemProperties().setProperty( MSBuildMojoITHelper.MSBUILD_PLUGIN_TOOLS_ENABLE, "true" );
        
        verifier.executeGoal( GROUPID + ":" + ARTIFACTID + ":" + CxxTestGenMojo.MOJO_NAME );
        verifier.verifyErrorFreeLog();
        assertTrue( "Test runner not generated", outputFile.exists() );
        verifier.resetStreams();
        // TODO: Add more tests to check the contents of our runner matches
        // the template we provided to cxxtestgen

        verifier.executeGoal( GROUPID + ":" + ARTIFACTID + ":" + CxxTestBuildMojo.MOJO_NAME );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        verifier.executeGoal( GROUPID + ":" + ARTIFACTID + ":" + CxxTestRunnerMojo.MOJO_NAME );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();        
    }

    private File calculateAndDeleteTestRunnerCpp( String directory ) throws IOException
    {
        final File outputDirectory = ResourceExtractor.simpleExtractResources( getClass(), 
                directory );
        File testRunnerCpp = new File( outputDirectory, "cxxtest-runner.cpp" );
        if ( testRunnerCpp.exists() )
        {
            testRunnerCpp.delete();
        }
        return testRunnerCpp;
    }

    /**
     * Create a modified version of the test runner source file with the expected absolute paths inserted.
     * @param testDirFile the directory where the test runner will be output
     * @param expectedSrc the expected file that needs replacements
     * @return a File for the generated file
     * @throws IOException if there is a problem manipulating files
     */
    private File createExpected( File testDirFile, File expectedSrc ) throws IOException
    {
        final File result = new File ( expectedSrc.getPath() + ".replaced" );

        BufferedReader src = null;
        BufferedWriter dest = null;
        try
        {
            String testDir = testDirFile.getPath();
            String testDirUnixSlash = testDir.replace( "\\", "/" );
            src = new BufferedReader( new InputStreamReader( new FileInputStream( expectedSrc ) ) );
            dest = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( result ) ) );
            String line = null;
            while ( ( line = src.readLine() ) != null )
            {
                line = line.replace( "${testDir}", testDir );
                line = line.replace( "${testDir.unixSlash}", testDirUnixSlash );
                dest.write( line );
                dest.newLine();
            }
        }
        finally
        {
            if ( src != null )
            {
                src.close();
            }
            if ( dest != null )
            {
                dest.close();
            }
        }
        return result;
    }

    
    private static final String GROUPID = "uk.org.raje.maven.plugins";
    private static final String ARTIFACTID = "msbuild-maven-plugin";
}

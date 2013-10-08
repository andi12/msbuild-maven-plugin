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

import junitx.framework.FileAssert;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Test;

/**
 * Test Sonar configuration generation.
 */
public class MavenITSonarConfigurationTest
{
    /**
     * Test simple configuration with no CppCheck or CxxTest
     * @throws Exception if there is a problem setting up and running Maven
     */
    @Test
    public void simpleConfig() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/sonar-config-test" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        addPropertiesToVerifier( verifier );
        
        verifier.executeGoal( SONAR_GOAL );
        verifier.verifyErrorFreeLog();

        FileAssert.assertEquals( 
                new File( testDir, "expected\\sonar-simple-Win32-Debug.properties" ), 
                new File( testDir, "target\\sonar-configuration-Win32-Debug.properties" ) );
        FileAssert.assertEquals( 
                new File( testDir, "expected\\sonar-simple-Win32-Release.properties" ), 
                new File( testDir, "target\\sonar-configuration-Win32-Release.properties" ) );
    }

    /**
     * Test config generation when CppCheck and CxxTest are availabe
     * @throws Exception if there is a problem setting up and running Maven
     */
    @Test
    public void simpleConfigWithCppCheckCxxTest() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/sonar-config-test" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        addPropertiesToVerifier( verifier );
        verifier.getSystemProperties().setProperty( MSBuildMojoITHelper.MSBUILD_PLUGIN_TOOLS_ENABLE, "true" );
        
        verifier.executeGoal( SONAR_GOAL );
        verifier.verifyErrorFreeLog();

        FileAssert.assertEquals( 
                new File( testDir, "expected\\sonar-simpleplus-Win32-Debug.properties" ), 
                new File( testDir, "target\\sonar-configuration-Win32-Debug.properties" ) );
        FileAssert.assertEquals( 
                new File( testDir, "expected\\sonar-simpleplus-Win32-Release.properties" ), 
                new File( testDir, "target\\sonar-configuration-Win32-Release.properties" ) );
    }

    /**
     * Test excludeProjectRegex setting 
     * @throws Exception if there is a problem setting up and running Maven
     */
    @Test
    public void excludes() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/sonar-config-test" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        addPropertiesToVerifier( verifier );
        verifier.addCliOption( "-f excludes-pom.xml" );
        
        verifier.executeGoal( SONAR_GOAL );
        verifier.verifyErrorFreeLog();

        FileAssert.assertEquals( 
                new File( testDir, "expected\\sonar-excludes-Win32-Debug.properties" ), 
                new File( testDir, "target\\sonar-configuration-Win32-Debug.properties" ) );
        FileAssert.assertEquals( 
                new File( testDir, "expected\\sonar-excludes-Win32-Release.properties" ), 
                new File( testDir, "target\\sonar-configuration-Win32-Release.properties" ) );
    }

    /**
     * Test that configured links are written out
     * @throws Exception if there is a problem setting up and running Maven
     */
    @Test
    public void links() throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/sonar-config-test" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        addPropertiesToVerifier( verifier );
        verifier.addCliOption( "-f links-pom.xml" );
        
        verifier.executeGoal( SONAR_GOAL );
        verifier.verifyErrorFreeLog();

        FileAssert.assertEquals( 
                new File( testDir, "expected\\sonar-links-Win32-Release.properties" ), 
                new File( testDir, "target\\sonar-configuration-Win32-Release.properties" ) );
    }

    private static final String SONAR_GOAL = "msbuild:sonar";
}

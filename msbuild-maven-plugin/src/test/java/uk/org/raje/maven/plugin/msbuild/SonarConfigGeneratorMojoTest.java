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
package uk.org.raje.maven.plugin.msbuild;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import org.junit.Test;

/**
 * Test CppCheckMojo configuration options.
 */
public class SonarConfigGeneratorMojoTest extends AbstractMSBuildMojoTestCase 
{
    public void setUp() throws Exception
    {
        super.setUp();
        
        outputStream = new ByteArrayOutputStream();
        System.setOut( new PrintStream( outputStream ) );
    }

    @Test
    public final void testAllSettingsConfiguration() throws Exception 
    {
        SonarConfigGeneratorMojo sonarConfigGeneratorMojo = ( SonarConfigGeneratorMojo ) 
                lookupConfiguredMojo( SonarConfigGeneratorMojo.MOJO_NAME, "/unit/configurations/allsettings-pom.xml" );

        assertAllSettingsConfiguration( sonarConfigGeneratorMojo );
    }

    public final void testSkipSonar() throws Exception 
    {
        SonarConfigGeneratorMojo sonarConfigGeneratorMojo = ( SonarConfigGeneratorMojo ) 
                lookupConfiguredMojo( SonarConfigGeneratorMojo.MOJO_NAME, "/unit/sonar/skip-sonar.pom" ) ;
        
        sonarConfigGeneratorMojo.execute();
        
        if ( ! outputStream.toString().trim().startsWith( SONAR_SKIP_MESSAGE ) ) 
        {
            fail();
        }
    }   
    
    public final void testMinimalSonarConfiguration() throws Exception 
    {
        SonarConfigGeneratorMojo sonarConfigGeneratorMojo = ( SonarConfigGeneratorMojo ) 
                lookupConfiguredMojo( SonarConfigGeneratorMojo.MOJO_NAME, "/unit/sonar/minimal-sonar-config.pom" ) ;
        
        sonarConfigGeneratorMojo.execute();
        assertTrue( "Expected output file not created", 
                new File( sonarConfigGeneratorMojo.mavenProject.getBuild().getDirectory(),
                        "sonar-configuration-Win32-Release.properties" ).exists() );
    }      
    
    private static final String SONAR_SKIP_MESSAGE = "[INFO] Skipping Sonar analysis";
    
    private ByteArrayOutputStream outputStream;
}

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
import java.io.PrintStream;

import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.logging.Logger;
import org.junit.Test;

import uk.org.raje.maven.plugin.msbuild.configuration.CppCheckConfiguration;

/**
 * Test CppCheckMojo configuration options.
 */
public class CppCheckMojoTest extends AbstractMSBuildMojoTestCase 
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
        CppCheckMojo cppCheckMojo = ( CppCheckMojo ) lookupConfiguredMojo( CppCheckMojo.MOJO_NAME, 
                "/unit/configurations/allsettings-pom.xml" );

        assertAllSettingsConfiguration( cppCheckMojo );
    }

    @Test
    public final void testMissingCppCheckConfiguration() throws Exception 
    {
        CppCheckMojo cppCheckMojo = ( CppCheckMojo ) lookupConfiguredMojo( CppCheckMojo.MOJO_NAME, 
                "/unit/cppcheck/missing-cppcheck-path-pom.xml" ) ;
        
        cppCheckMojo.execute();
    }
    
    @Test
    public final void testMinimalSolutionConfiguration() throws Exception 
    {
        ( (DefaultPlexusContainer) getContainer() ).getLoggerManager().setThreshold( Logger.LEVEL_DEBUG );
        CppCheckMojo cppCheckMojo = ( CppCheckMojo ) lookupConfiguredMojo( CppCheckMojo.MOJO_NAME, 
                "/unit/cppcheck/sln-single-platform-single-config-pom.xml" );

        cppCheckMojo.execute();
    }    

    @Test
    public final void testSolutionExcludesConfiguration() throws Exception 
    {
        CppCheckMojo cppCheckMojo = ( CppCheckMojo ) lookupConfiguredMojo( CppCheckMojo.MOJO_NAME, 
                "/unit/cppcheck/sln-single-platform-single-config-excludes-pom.xml" );

        cppCheckMojo.execute();
        
        assertTrue( CppCheckConfiguration.CPPCHECK_NAME + " execution was not skipped", 
                outputStream.toString().trim().equals( "[INFO] Static code analysis complete" ) );
    }    

    private ByteArrayOutputStream outputStream;
}

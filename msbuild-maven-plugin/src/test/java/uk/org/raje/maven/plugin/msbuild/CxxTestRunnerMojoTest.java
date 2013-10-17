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

import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;

/**
 * Test CxxTestGenMojo configuration options.
 */
public class CxxTestRunnerMojoTest extends AbstractMSBuildMojoTestCase 
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
    public final void testMissingCxxTestHomePath() throws Exception 
    {
        CxxTestRunnerMojo cxxTestRunnerMojo = ( CxxTestRunnerMojo ) lookupConfiguredMojo( CxxTestRunnerMojo.MOJO_NAME, 
                "/unit/cxxtest/missing-cxxtest-home-path.pom" ) ;
        
        cxxTestRunnerMojo.execute();
        
        if ( !outputStream.toString().contains( CXXTEST_SKIP_MESSAGE ) )
        {
            fail();
        }
    }    

    public final void testSkipCxxTest() throws Exception 
    {
        CxxTestRunnerMojo cxxTestRunnerMojo = ( CxxTestRunnerMojo ) lookupConfiguredMojo( CxxTestRunnerMojo.MOJO_NAME, 
                "/unit/cxxtest/skip-cxxtest.pom" ) ;
        
        try
        {
            cxxTestRunnerMojo.execute();
        }
        catch ( AbstractMojoExecutionException ame )
        {
            fail( ame.getCause() != null ? ame.getCause().getMessage() : ame.getMessage() );
        }
        
        if ( !outputStream.toString().contains( CXXTEST_SKIP_MESSAGE ) ) 
        {
            fail();
        }
    }    

    
    @Test
    public final void testPassingCxxTest() throws Exception 
    {
        CxxTestRunnerMojo cxxTestRunnerMojo = ( CxxTestRunnerMojo ) lookupConfiguredMojo( CxxTestRunnerMojo.MOJO_NAME, 
                "/unit/cxxtest/passing-test-config.pom" ) ;
        
        try
        {
            cxxTestRunnerMojo.execute();
        }
        catch ( AbstractMojoExecutionException ame )
        {
            fail( ame.getCause() != null ? ame.getCause().getMessage() : ame.getMessage() );
        }
    }

    @Test
    public final void testFailingCxxTest() throws Exception 
    {
        CxxTestRunnerMojo cxxTestRunnerMojo = ( CxxTestRunnerMojo ) lookupConfiguredMojo( CxxTestRunnerMojo.MOJO_NAME, 
                "/unit/cxxtest/failing-test-config.pom" ) ;
        
        try
        {
            cxxTestRunnerMojo.execute();
        }
        catch ( MojoFailureException mfe )
        {
            return;
        }
        catch ( AbstractMojoExecutionException ame )
        {
            fail( ame.getCause() != null ? ame.getCause().getMessage() : ame.getMessage() );
        }
        
        fail ( "Expected CxxTest failure." );
    }

    private static final String CXXTEST_SKIP_MESSAGE = "Skipping test";
    
    private ByteArrayOutputStream outputStream;
}

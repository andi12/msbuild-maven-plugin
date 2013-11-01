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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;

/**
 * Test VeraMojo configuration options.
 */
public class VeraMojoTest extends AbstractMSBuildMojoTestCase 
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
        VeraMojo veraMojo = ( VeraMojo ) lookupConfiguredMojo( VeraMojo.MOJO_NAME, 
                "/unit/configurations/allsettings-pom.xml" );

        assertAllSettingsConfiguration( veraMojo );
    }

    @Test
    public final void testSkipVeraExecution() throws Exception 
    {
        VeraMojo veraMojo = ( VeraMojo ) lookupConfiguredMojo( VeraMojo.MOJO_NAME, 
                "/unit/vera/skip-vera.pom" ) ;
        
        veraMojo.execute();
        
        if ( !outputStream.toString().contains( VERA_SKIP_MESSAGE ) ) 
        {
            fail();
        }
    }   
    
    @Test
    public final void testMissingVeraHomePath() throws Exception 
    {
        VeraMojo veraMojo = ( VeraMojo ) lookupConfiguredMojo( VeraMojo.MOJO_NAME, 
                "/unit/vera/missing-vera-home-path.pom" ) ;
        
        veraMojo.execute();
        
        if ( !outputStream.toString().contains( VERA_SKIP_MESSAGE ) )
        {
            fail();
        }
    }    
    
    @Test
    public final void testGetVeraHomePathFromSystemProperties() throws Exception 
    {
        System.setProperty( VERA_HOME_PROPERTY, "src/test/resources/unit/vera/fake-vera-home" );
        VeraMojo veraMojo = ( VeraMojo ) lookupConfiguredMojo( VeraMojo.MOJO_NAME, 
                "/unit/vera/missing-vera-home-path.pom" ) ;

        try
        {
            veraMojo.execute();
        }
        finally
        {
            System.getProperties().remove( VERA_HOME_PROPERTY );
        }
        
        if ( outputStream.toString().contains( VERA_SKIP_MESSAGE ) )
        {
            fail( VERA_HOME_PROPERTY + " could not be found in the system properties" );
        }
    }    

    @Test
    public final void testVeraMinimalConfig()
            throws Exception
    {
        VeraMojo veraMojo = ( VeraMojo ) lookupConfiguredMojo( VeraMojo.MOJO_NAME, 
                "/unit/vera/minimal-vera-config.pom" );
        
        testVeraExecution( veraMojo, Collections.<String, String> emptyMap() );
    }

    @Test
    public final void testVeraParametersConfig()
            throws Exception
    {
        Map<String, String> testParameters = new HashMap<String, String>();
        testParameters.put( "max-line-length", "120" );
        testParameters.put( "max-filename-length", "40" );
        
        VeraMojo veraMojo = ( VeraMojo ) lookupConfiguredMojo( VeraMojo.MOJO_NAME, 
                "/unit/vera/vera-parameters-config.pom" );

        testVeraExecution( veraMojo, testParameters );
    }

    
    private void testVeraExecution( VeraMojo veraMojo, Map<String, String> expectedParameters )
        throws MojoFailureException, MojoExecutionException
    {
        Map<String, String> parameters = new HashMap<String, String>();
        
        veraMojo.execute();
        List<String> infoLogMessages = getTaggedLogMessages( outputStream.toString(), LogMessageTag.INFO );
        
        assertEquals( "Running coding style analysis for project hello-world, platform=Win32, configuration=Release", 
                infoLogMessages.remove( 0 ) );
        
        File veraToolPath = new File( infoLogMessages.remove( 0 ) );
        File veraHome = veraToolPath.getParentFile().getParentFile(); 
        assertEquals( veraToolPath, new File( veraHome, "bin/vera++.exe" ) );

        assertEquals( "--root", infoLogMessages.remove( 0 ) );
        
        File rootDir = new File( infoLogMessages.remove( 0 ) );
        assertEquals( rootDir, new File( veraHome, "lib/vera++" ) );

        assertEquals( "--profile", infoLogMessages.remove( 0 ) );
        assertEquals( "full", infoLogMessages.remove( 0 ) );

        assertEquals( "--checkstyle-report", infoLogMessages.remove( 0 ) );
        assertEquals( "-", infoLogMessages.remove( 0 ) );
        
        for ( int i = 0; i < expectedParameters.size(); i++ )
        {
            String parameterLine = infoLogMessages.remove( 0 );
            assertEquals( "--parameter", parameterLine );
            
            String parameter[] = infoLogMessages.remove( 0 ).split( "=" );
            parameters.put( parameter[0], parameter[1] );
        }

        assertEquals( expectedParameters, parameters );
        assertEquals( "--warning", infoLogMessages.remove( 0 ) );
        assertEquals( "--quiet", infoLogMessages.remove( 0 ) );

        assertEquals( "hello-world-project\\hello-world.cpp", infoLogMessages.remove( 0 ) );

        assertEquals( "Coding style analysis complete", infoLogMessages.remove( 0 ) );
        assertEquals( infoLogMessages.size(), 0 );
    }
    
    private static final String VERA_SKIP_MESSAGE = "Skipping coding style analysis";
    private static final String VERA_HOME_PROPERTY = "vera.home";

    private ByteArrayOutputStream outputStream;
}

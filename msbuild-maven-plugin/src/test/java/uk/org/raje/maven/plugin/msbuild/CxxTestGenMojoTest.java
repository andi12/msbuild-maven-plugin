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
import org.junit.Test;

/**
 * Test CxxTestGenMojo configuration options.
 */
public class CxxTestGenMojoTest extends AbstractMSBuildMojoTestCase 
{
    public void setUp() throws Exception
    {
        super.setUp();
        
        outputStream = new ByteArrayOutputStream();
        System.setOut( new PrintStream( outputStream ) );
    }
    
    @Test
    public final void testMissingCxxTestHomePath() throws Exception 
    {
        CxxTestGenMojo cxxTestGenMojo = ( CxxTestGenMojo ) lookupConfiguredMojo( CxxTestGenMojo.MOJO_NAME, 
                "/unit/cxxtest/missing-cxxtest-home-path.pom" ) ;
        
        try
        {
            cxxTestGenMojo.execute();
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

    public final void testSkipCxxTest() throws Exception 
    {
        CxxTestGenMojo cxxTestGenMojo = ( CxxTestGenMojo ) lookupConfiguredMojo( CxxTestGenMojo.MOJO_NAME, 
                "/unit/cxxtest/skip-cxxtest.pom" ) ;
        
        try
        {
            cxxTestGenMojo.execute();
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
    public final void test() throws Exception 
    {
        CxxTestGenMojo cxxTestGenMojo = ( CxxTestGenMojo ) lookupConfiguredMojo( CxxTestGenMojo.MOJO_NAME, 
                "/unit/cxxtest/minimal-cxxtestgen-config.pom" ) ;
        
        try
        {
            cxxTestGenMojo.execute();
        }
        catch ( AbstractMojoExecutionException ame )
        {
            fail( ame.getCause() != null ? ame.getCause().getMessage() : ame.getMessage() );
        }
    }
    
    private static final String CXXTEST_SKIP_MESSAGE = "Skipping test";

    private ByteArrayOutputStream outputStream;
}

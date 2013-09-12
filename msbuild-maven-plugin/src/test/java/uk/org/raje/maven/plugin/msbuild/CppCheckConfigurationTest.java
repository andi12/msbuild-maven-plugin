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

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;

/**
 * Test CppCheckMojo configuration options.
 */
public class CppCheckConfigurationTest extends AbstractMSBuildMojoTestCase 
{
    
    @Before
    protected void setUp() throws Exception 
    {
        // required for mojo lookups to work
        super.setUp();
    }    
    
    @Test
    public final void testMissingCppCheckConfiguration() throws Exception 
    {
        CppCheckMojo cppCheckMojo = ( CppCheckMojo ) lookupConfiguredMojo( CppCheckMojo.MOJO_NAME, 
                "src/test/resources/unit/cppcheck/no-cppcheck-path-pom.xml" );
        try
        {
            cppCheckMojo.execute();
        }
        catch ( MojoExecutionException mee )
        {
            fail();
        }
    }
    
    @Test
    public final void testMinimalSolutionConfiguration() throws Exception 
    {
        CppCheckMojo cppCheckMojo = ( CppCheckMojo ) lookupConfiguredMojo( CppCheckMojo.MOJO_NAME, 
                "src/test/resources/unit/cppcheck/sln-single-platform-single-config-pom.xml" );
        
        try
        {
            cppCheckMojo.execute();
        }
        catch ( MojoExecutionException mee )
        {
            fail();
        }
    }    
}

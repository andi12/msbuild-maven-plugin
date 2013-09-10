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

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.junit.Test;

/**
 * Test CppCheckMojo configuration options.
 */
public class CppCheckConfigurationTest extends AbstractMojoTestCase 
{
    @Test
    public final void testMissingCppCheckConfiguration() throws Exception 
    {
        CppCheckMojo cppCheckMojo = 
                lookupCppCheckMojo( "src/test/resources/unit/cppcheck/no-cppcheck-path-pom.xml" );
        try
        {
            cppCheckMojo.execute();
            fail();
        }
        catch ( MojoExecutionException mee )
        {
            assertEquals( mee.getCause().getClass(), FileNotFoundException.class );
        }
    }
    
    @Test
    public final void testMinimalSolutionConfiguration() throws Exception 
    {
        CppCheckMojo cppCheckMojo = 
                lookupCppCheckMojo( "src/test/resources/unit/cppcheck/minimal-msbuild-solution-pom.xml" );
        
        try
        {
            cppCheckMojo.execute();
        }
        catch ( MojoExecutionException mee )
        {
            fail();
        }
    }    

    /**
     * Workaround for parent class lookupMojo and lookupConfiguredMojo.
     * @param pomPath where to find the POM file
     * @return a configured MSBuild Mojo for testing
     * @throws Exception if we can't find the Mojo or the POM is malformed
     */
    protected final CppCheckMojo lookupCppCheckMojo( String pomPath ) throws Exception
    {
        File pom = getTestFile( pomPath );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        // The following 4 lines are simply to get a MavenProject object
        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        ProjectBuildingRequest buildingRequest = executionRequest.getProjectBuildingRequest();
        ProjectBuilder projectBuilder = this.lookup( ProjectBuilder.class );
        MavenProject mavenProject = projectBuilder.build( pom, buildingRequest ).getProject();
        assertNotNull( mavenProject );
        
        // Used lookupMojo as it sets up most of what we need and reads configuration
        // variables from the poms.
        // It doesn't set a MavenProject so we have to do that manually
        // lookupConfiguredMojo doesn't work properly, configuration variables are no expanded
        // as we expect and it fails to setup a Log.
        //CppCheckMojo cppCheckMojo = (CppCheckMojo) lookupConfiguredMojo( mavenProject, CppCheckMojo.MOJO_NAME );
        CppCheckMojo cppCheckMojo = (CppCheckMojo) lookupMojo( CppCheckMojo.MOJO_NAME, pom );
        assertNotNull( cppCheckMojo );
        cppCheckMojo.mavenProject = mavenProject;

        return cppCheckMojo;
    }
}

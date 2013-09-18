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
import java.util.Arrays;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildConfiguration;
import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;

/**
 * Abstract unit test base class to extend AbstractMojoTestCase and add 
 * common functions that we use.
 */
public abstract class AbstractMSBuildMojoTestCase extends AbstractMojoTestCase
{

    /**
     * Test that all Mojo's should run to check they can ingest a complete configuration 
     */
    public final void assertAllSettingsConfiguration( AbstractMSBuildPluginMojo m ) throws Exception 
    {
        assertEquals( Arrays.asList( new BuildPlatform( "Win32" ) ), m.platforms );
        assertEquals( Arrays.asList( new BuildConfiguration( "Release" ), new BuildConfiguration( "Debug" ) ), 
                m.platforms.get( 0 ).getConfigurations() );
    }

    /**
     * Workaround for parent class lookupMojo and lookupConfiguredMojo.
     * @param name the name of the Mojo to lookup
     * @param pomPath where to find the POM file
     * @return a configured MSBuild Mojo for testing
     * @throws Exception if we can't find the Mojo or the POM is malformed
     */
    protected final Mojo lookupConfiguredMojo( String name, String pomPath ) throws Exception
    {
        File pom = new File( getClass().getResource( pomPath ).getPath() );
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
        Mojo mojo = lookupMojo( name, pom );
        //Mojo mojo = super.lookupConfiguredMojo( mavenProject, name );
        assertNotNull( mojo );

        setVariableValueToObject( mojo, "mavenProject", mavenProject );
        
        return mojo;
    }
}
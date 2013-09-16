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
import java.lang.reflect.Field;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.DefaultContext;

/**
 * Abstract unit test base class to extend AbstractMojoTestCase and add 
 * common functions that we use.
 */
public abstract class AbstractMSBuildMojoTestCase extends AbstractMojoTestCase
{

    /**
     * Workaround for parent class lookupMojo and lookupConfiguredMojo.
     * @param name the name of the Mojo to lookup
     * @param pomPath where to find the POM file
     * @return a configured MSBuild Mojo for testing
     * @throws Exception if we can't find the Mojo or the POM is malformed
     */
    protected final Mojo lookupConfiguredMojo( String name, String pomPath ) throws Exception
    {
        File pom = getTestFile( pomPath );
        assertNotNull( pom );
        assertTrue( pom.exists() );


        // The following is an attempt to create a MavenProject
        // This fails because the Context is not properly initialised
        // I am committing this to record how far this attempt got
        ProjectBuilderConfiguration projectBuilderConfiguration = new DefaultProjectBuilderConfiguration();
        Context context = new DefaultContext();

        MavenProjectBuilder projectBuilder = new DefaultMavenProjectBuilder();
        ( ( DefaultMavenProjectBuilder ) projectBuilder ).initialize();
        ( ( DefaultMavenProjectBuilder ) projectBuilder ).contextualize( context );
        MavenProject mavenProject = projectBuilder.build( pom, projectBuilderConfiguration );
        assertNotNull( mavenProject );
        
        // Used lookupMojo as it sets up most of what we need and reads configuration
        // variables from the poms.
        // It doesn't set a MavenProject so we have to do that manually
        // lookupConfiguredMojo doesn't work properly, configuration variables are no expanded
        // as we expect and it fails to setup a Log.
        Mojo mojo = lookupMojo( name, pom );
        //Mojo mojo = lookupConfiguredMojo( mavenProject, name );
        assertNotNull( mojo );

        Class<? extends Mojo> clazz = mojo.getClass();
        while ( true )
        {
            try
            {
                Field field = clazz.getDeclaredField( "mavenProject" );
                field.set( mojo, mavenProject );
                break;
            }
            catch ( NoSuchFieldException nsfe )
            {
                clazz = (Class<? extends Mojo>) clazz.getSuperclass();
                if ( clazz == null )
                {
                    fail();
                }
                
            }
        }
        
        return mojo;
    }
}

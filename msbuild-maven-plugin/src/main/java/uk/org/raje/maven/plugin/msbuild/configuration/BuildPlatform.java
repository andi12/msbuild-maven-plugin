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
package uk.org.raje.maven.plugin.msbuild.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Configuration container for the &lt;platform&gt; element  
 */
public class BuildPlatform
{
    /**
     * Constant for the default platform name.
     */
    public static final String DEFAULT_PLATFORM = "Win32";

    /**
     * Construct a default BuildPlatform.
     */
    public BuildPlatform()
    {
        name = DEFAULT_PLATFORM;
        configurations = new ArrayList<BuildConfiguration>( Arrays.asList( RELEASE_CONFIGURATION ) );
    }

    /**
     * Construct a BuildPlatform with the specified name.
     * @param name the platform name
     */
    public BuildPlatform( String name )
    {
        this.name = name;
        configurations = new ArrayList<BuildConfiguration>( Arrays.asList( RELEASE_CONFIGURATION ) );
    }

    @Override
    public String toString()
    {
        return name;
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public boolean equals( Object o )
    {
        if ( name != null && name.equals( ( ( BuildPlatform ) o ).name ) )
        {
            return true;
        }
        return false;
    }

    /**
     * Get the name of this platform
     * @return the platform name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get the list of BuildConfiguration's for this platform
     * @return the build configurations
     */
    public List<BuildConfiguration> getConfigurations()
    {
        if ( configurations.isEmpty() )
        {
            configurations.add( new BuildConfiguration() );
        }
        return configurations;
    }

    /**
     * Check the configurations and mark one as primary.
     * @throws MojoExecutionException if duplicate configuration names are found
     */
    public void identifyPrimaryConfiguration() throws MojoExecutionException
    {
        Set<String> configurationNames = new HashSet<String>();
        for ( BuildConfiguration configuration : configurations )
        {
            if ( configurationNames.contains( configuration.getName() ) )
            {
                throw new MojoExecutionException( "Duplicate configuration '" + configuration.getName()
                        + "' for '" + getName() + "', configuration names must be unique" );
            }
            configurationNames.add( configuration.getName() );
            configuration.setPrimary( false );
        }
        if ( configurations.contains( RELEASE_CONFIGURATION ) )
        {
            configurations.get( configurations.indexOf( RELEASE_CONFIGURATION ) ).setPrimary( true );
        }
        else if ( configurations.contains( DEBUG_CONFIGURATION ) )
        {
            configurations.get( configurations.indexOf( DEBUG_CONFIGURATION ) ).setPrimary( true );
        }
        else
        {
            configurations.get( 0 ).setPrimary( true );
        }
    }
    
    private static final BuildConfiguration RELEASE_CONFIGURATION = new BuildConfiguration( "Release" );
    private static final BuildConfiguration DEBUG_CONFIGURATION = new BuildConfiguration( "Debug" );

    @Parameter
    private String name;
    
    @Parameter
    private List<BuildConfiguration> configurations;
}

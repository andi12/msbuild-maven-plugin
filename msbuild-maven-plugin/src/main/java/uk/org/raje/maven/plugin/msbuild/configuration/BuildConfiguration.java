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

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Configuration container for the &lt;configuration&gt; element  
 */
public class BuildConfiguration
{
    /**
     * Constant for the default configuration name.
     */
    public static final String DEFAULT_CONFIGURATION = "Release";

    /**
     * Construct a default BuildConfiguration.
     */
    public BuildConfiguration()
    {
        name = DEFAULT_CONFIGURATION;
    }

    /**
     * Construct a BuildConfiguration with the specified name.
     * @param name the configuration name
     */
    public BuildConfiguration( String name )
    {
        this.name = name;
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
        if ( name != null && name.equals( ( ( BuildConfiguration ) o ).name ) )
        {
            return true;
        }
        return false;
    }

    /**
     * Get the name of this configuration
     * @return the configuration name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Is this configuration the primary configuration for the platform
     * @return true if this is the primary configuration
     */
    public boolean isPrimary()
    {
        return isPrimary;
    }

    /**
     * Allow only classes in our package to set the primary flag
     * @param b the new value for isPrimary
     */
    protected void setPrimary( boolean b )
    {
        isPrimary = b;
    }
    
    @Parameter
    private String name;

    //@Parameter
    //private FileSet outputs;

    private boolean isPrimary;
}

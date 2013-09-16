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

import java.io.File;
import java.util.List;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Configuration holder for cxxtest configuration values.
 */
public class CxxTestConfiguration
{
    /**
     * Get the configured value for skip 
     * @return the configured value or false if not configured
     */
    public final boolean skip()
    {
        return skip;
    }

    /**
     * Get the configured path to cxxtestgen
     * @return the path to cxxtestgen
     */
    public final File cxxTestGenPath()
    {
        return cxxTestGenPath;
    }

    /**
     * Get the configured List of test targets
     * @return the List of test targets
     */
    public final List<String> testTargets()
    {
        return testTargets;
    }

    /**
     * Set to true to skip CxxTest functionality.
     */
    @Parameter( 
            defaultValue = "false", 
            readonly = false )
    private boolean skip = false; 

    /**
     * The path to the cxxtestgen script.
     */
    @Parameter( 
            property = "cxxtestgen.path", 
            readonly = false, 
            required = false )
    private File cxxTestGenPath;

    /**
     * The set of test targets (projects) to build.
     */
    @Parameter(
            readonly = false,
            required = false )
    protected List<String> testTargets;
}

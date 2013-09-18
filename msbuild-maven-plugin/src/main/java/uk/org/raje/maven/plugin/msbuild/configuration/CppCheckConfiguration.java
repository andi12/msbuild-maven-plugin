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

import org.apache.maven.plugins.annotations.Parameter;

import uk.org.raje.maven.plugin.msbuild.CppCheckType;

/**
 * Configuration holder for CppCheck configuration values.
 */
public class CppCheckConfiguration
{

    /**
     * The name of the environment variable that can store the PATH TO CppCheck.
     */
    public static final String CPPCHECK_PATH_ENVVAR = "CPPCHECK_PATH";
    
    /**
     * The CppCheck name to output on debug/information messages
     */    
    public static final String CPPCHECK_NAME = "CppCheck";
    
    /**
     * Get the configured value for skip 
     * @return the configured value or false if not configured
     */
    public final boolean skip()
    {
        return skip;
    }

    /**
     * Get the configured path to CppCheck
     * @return the path to CppCheck
     */
    public final File cppCheckPath()
    {
        return cppCheckPath;
    }

    /**
     * Get the configured report name prefix
     * @return the prefix for report file names
     */
    public final String reportName()
    {
        return reportName;
    }

    /**
     * Get the configured check type, if not configured the detault CppCheckType.all is returned.
     * @return the CppCheckType
     */
    public final CppCheckType cppCheckType()
    {
        return cppCheckType;
    }

    /**
     * Get the configured exclusion regex, may be null.
     * @return the configured String or null
     */
    public final String excludeProjectRegex()
    {
        return excludeProjectRegex;
    }

    /**
     * Set to true to skip CppCheck functionality.
     */
    @Parameter( 
            defaultValue = "false", 
            readonly = false )
    private boolean skip = false; 

    /**
     * The path to CppCheck.
     * Note: The property is not specified here as it doesn't work.
     * @see uk.org.raje.maven.plugin.msbuild.AbstractMSBuildPluginMojo#cppCheckPath 
     */
    @Parameter( 
            readonly = false, 
            required = false )
    private File cppCheckPath;

    /**
     * The prefix for the CppCheck report output file.
     */
    @Parameter( 
            defaultValue = "cppcheck-report", 
            readonly = false, 
            required = false )
    private String reportName = "cppcheck-report";
    
    @Parameter( 
            readonly = false, 
            required = false )
    private CppCheckType cppCheckType = CppCheckType.all;
    
    @Parameter( 
            readonly = false, 
            required = false )
    private String excludeProjectRegex;
}

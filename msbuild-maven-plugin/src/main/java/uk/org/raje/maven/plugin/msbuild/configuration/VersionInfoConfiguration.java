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
import java.util.Calendar;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Configuration holder for VersionInfo configuration values.
 */
public class VersionInfoConfiguration
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
     * Get the configured companyName
     * @return the companyName
     */
    public final String getCompanyName()
    {
        return companyName;
    }

    /**
     * Get the configured copyright string
     * @return copyright string or default if not configured
     */
    public final String getCopyright()
    {
        if ( copyright == null )
        {
            StringBuilder sb = new StringBuilder();
            sb.append( COPYRIGHT_PREAMBLE )
              .append( " " )
              .append( Calendar.getInstance().get( Calendar.YEAR ) )
              .append( " " )
              .append( companyName );
            copyright = sb.toString();
        }
        return copyright;
    }

    /**
     * Get the configured template file path
     * @return an abstract path to the template file
     */
    public final File getTemplate()
    {
        return template;
    }

    /**
     * Get the configured output file
     * @return the configured value, if not configured the default is returned
     */
    public final File getOutputFile()
    {
        return outputFile;
    }

    private static final String COPYRIGHT_PREAMBLE = "Copyright (c)";
    /**
     * The filename for the generated file.
     */
    private static final String DEFAULT_VERSION_INFO_FILENAME = "maven-version-info.rc";
    
    /**
     * Set to true to skip creating a version-info.rc from POM properties.
     */
    @Parameter( 
            defaultValue = "false", 
            readonly = false )
    private boolean skip = false; 

    /**
     * The company name string to use, if not provided version-info will be skipped.
     */
    @Parameter(
            readonly = false,
            required = false )
    private String companyName;

    /**
     * The copyright string to use, defaults to 'Copyright (c) [this year] [companyName]'
     */
    @Parameter(
            readonly = false,
            required = false )
    private String copyright;

    /**
     * Override the default template to use your own.
     * <p>
     * The file will be processed to replace any Maven style property placeholders (${property.Name}) with
     * their values. In addition to any properties available to the build the plugin will supply the following:
     * <ul>
     *   <li>version-info.companyname</li>
     *   <li>version-info.copyright</li>
     *   <li>version-info.majorVersion (an integer)</li>
     *   <li>version-info.minorVersion (an integer)</li>
     *   <li>version-info.incrementalVersion (an integer)</li>
     *   <li>version-info.buildNumber (an integer)</li>
     * </ul>
     */
    @Parameter(
            readonly = false,
            required = false )
    private File template;

    /**
     * The path and filename to output the version resource to.
     * <p>
     * If you specify a relative path the file will be output to that path relative to the project or solution file.
     */
    @Parameter(
            defaultValue = DEFAULT_VERSION_INFO_FILENAME,
            readonly = false,
            required = false )
    private File outputFile = new File( DEFAULT_VERSION_INFO_FILENAME );
}

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

package uk.org.raje.maven.plugin.msbuild.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;

/**
 * Abstract base class for Visual Studio solution/project parsing functionality.
 */
abstract class BaseParser 
{
    /**
     * @param inputFile the file to parse, typically a Visual Studio solution file or a Visual Studio project file
     * @param platform the platform to target during parsing (for example, {@code Win32}, {@code x64})
     * @param configuration the configuration to target during parsing (for example, {@code Release}, {@code Debug})
     * @throws FileNotFoundException if the given solution file is not found
     */
    public BaseParser( File inputFile, String platform, String configuration ) 
            throws FileNotFoundException
    {
        if ( inputFile == null ) 
        {
            throw new FileNotFoundException( "No input file specified." );
        }
        
        if ( !inputFile.exists() || !inputFile.isFile() ) 
        {
            throw new FileNotFoundException( inputFile.getAbsolutePath() );
        }

        this.inputFile = inputFile;
        this.configuration = configuration;
        this.platform = platform;
    }
    
    /**
     * Return the {@link File} to parse.
     * @return the {@link File} to parse
     */
    public File getInputFile() 
    {
        return inputFile;
    }
    
    /**
     * Return the platform to target during parsing (for example, {@code Win32}, {@code x64}).
     * @return the platform to target during parsing
     */
    public String getPlatform() 
    {
        return platform;
    }

    /**
     * Return the configuration to target during parsing (for example, {@code Release}, {@code Debug}). 
     * @return the configuration to target during parsing
     */
    public String getConfiguration() 
    {
        return configuration;
    }

    /**
     * Return the platform and configuration to target during parsing in then format <em>configuration|platform</em>.
     * @return the platform and configuration to target during parsing
     */
    public String getConfigurationPlatform() 
    {
        return configuration.replaceAll( "[ \t]" , "" ) + "|" + platform.replaceAll( "[ \t]" , "" );
    }
    
    /**
     * Parse the input file.
     * @throws IOException if the input file does not exists or cannot be accessed
     * @throws ParseException if an error occurs during parsing
     */
    public abstract void parse() throws IOException, ParseException;
    
    private String platform;
    private String configuration;
    private File inputFile;
}

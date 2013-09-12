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
import java.security.InvalidParameterException;
import java.text.ParseException;


/**
 * @author dmasato
 *
 */
public abstract class BaseParser 
{
    public BaseParser( File inputFile, String platform, String configuration ) throws FileNotFoundException 
    {
        if ( inputFile == null ) 
        {
            throw new InvalidParameterException();
        }
        
        if ( !inputFile.exists() || !inputFile.isFile() ) 
        {
            throw new FileNotFoundException( inputFile.getAbsolutePath() );
        }

        this.inputFile = inputFile;
        requiredConfig = configuration;
        requiredPlatform = platform;
    }
    
    public File getInputFile() 
    {
        return inputFile;
    }

    public File getInputFileParent() 
    {
        return inputFile.getParentFile();
    }

    public String getRequiredConfiguration() 
    {
        return requiredConfig;
    }

    public String getRequiredPlatform() 
    {
        return requiredPlatform;
    }

    public String getRequiredConfigurationPlatform() 
    {
        return requiredConfig + "|" + requiredPlatform;
    }
    
    public abstract void parse() throws IOException, ParseException;
    
    private String requiredPlatform;
    private String requiredConfig;
    private File inputFile;
}

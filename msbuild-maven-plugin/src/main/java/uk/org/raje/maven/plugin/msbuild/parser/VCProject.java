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
import java.security.InvalidParameterException;
import java.util.List;


/**
 * @author dmasato
 *
 */
public class VCProject 
{
    public VCProject( String name, File path ) 
    {
        if ( name == null || path == null ) 
        {
            throw new InvalidParameterException();
        }
        
        this.name = name;
        this.path = path.getAbsoluteFile();
    }
    
    public String getGuid() 
    {
        return guid;
    }
        
    public void setGuid( String guid ) 
    {
        this.guid = guid;
    }

    public String getSolutionGuid() 
    {
        return solutionGuid;
    }
    
    public void setSolutionGuid( String solutionGuid ) 
    {
        this.solutionGuid = solutionGuid;
    }
    
    public String getName() 
    {
        return name;
    }
    
    public File getPath() 
    {
        return path;
    }

    public File getBaseDir() 
    {
        return getPath().getParentFile();
    }
    
    public String getConfiguration() 
    {
        return configuration;
    }
    
    public void setConfiguration( String configuration ) 
    {
        this.configuration = configuration;
    }
    
    public String getPlatform() 
    {
        return platform;
    }
    
    public void setPlatform( String platform ) 
    {
        this.platform = platform;
    }
    
    public List<String> getPreprocessorDefs() 
    {
        return preprocessorDefs;
    }

    public void setPreprocessorDefs( List<String> preprocessorDefs ) 
    {
        this.preprocessorDefs = preprocessorDefs;
    }

    public List<String> getIncludeDirectories() 
    {
        return includeDirectories;
    }

    public void setIncludeDirectories( List<String> includeDirectories ) 
    {
        this.includeDirectories = includeDirectories;
    }

    private String guid = null;
    private String solutionGuid = null;
    private String name = null;
    private File path = null;
    private String configuration = null;
    private String platform = null;
    private List<String> includeDirectories = null;
    private List<String> preprocessorDefs = null;
}
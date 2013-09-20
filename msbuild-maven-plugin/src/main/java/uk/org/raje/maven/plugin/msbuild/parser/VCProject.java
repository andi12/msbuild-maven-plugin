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
 * Bean to hold properties parsed from a Visual Studio project file.
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

    /**
     * Get the value of the configured 'Output Directory'
     * @return the string value
     */
    public File getOutDir()
    {
        if ( outDir == null )
        {
            if ( "Win32".equals( platform ) )
            {
                // Win32 is a special case in VS
                outDir = new File( path.getParentFile(), configuration );
            }
            else
            {
                outDir = new File( path.getParentFile(), platform + File.separator + configuration );
            }
        }
        return outDir;
    }

    /**
     * Set the stored value for outDir
     * @param outDir the new value
     */
    protected void setOutDir( String outDir )
    {
        if ( outDir == null )
        {
            this.outDir = null;
        }
        else
        {
            this.outDir = new File( outDir ); 
            if ( ! this.outDir.isAbsolute() )
            {
                outDir = outDir.replace( "$(SolutionDir)", "" );
                this.outDir = new File( path.getParent(), outDir );
            }
        }
    }

    public List<String> getIncludeDirectories() 
    {
        return includeDirectories;
    }

    protected void setIncludeDirectories( List<String> includeDirectories ) 
    {
        this.includeDirectories = includeDirectories;
    }

    public List<String> getPreprocessorDefs() 
    {
        return preprocessorDefs;
    }

    protected void setPreprocessorDefs( List<String> preprocessorDefs ) 
    {
        this.preprocessorDefs = preprocessorDefs;
    }


    private String guid;
    private String solutionGuid;
    private String name;
    private File path;
    private String configuration;
    private String platform;
    private File outDir;
    private List<String> includeDirectories;
    private List<String> preprocessorDefs;
}

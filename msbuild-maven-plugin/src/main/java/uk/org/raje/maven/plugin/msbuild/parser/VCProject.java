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
import java.util.List;

/**
 * Bean to hold properties parsed from a Visual C++ project file.
 */
public class VCProject 
{
    public VCProject( String name, File projectFile, String platform, String configuration ) 
    {
        this.name = name;
        this.projectFile = projectFile;
        this.platform = platform;
        this.configuration = configuration;
    }

    protected VCProject( String name, File projectFile ) 
    {
        this.name = name;
        this.projectFile = projectFile;
    }

    public String getGuid() 
    {
        return guid;
    }
        
    protected void setGuid( String guid ) 
    {
        this.guid = guid;
    }

    public String getSolutionGuid() 
    {
        return solutionGuid;
    }
    
    protected void setSolutionGuid( String solutionGuid ) 
    {
        this.solutionGuid = solutionGuid;
    }

    /**
     * Return the name of the project
     * @return
     */
    public String getName()
    {
        return name;
    }
 
    /**
     * Return the target name that indicates this project.
     * Only valid for projects found via a Solution file.
     * @return the target name or null if there was no solution file
     */
    public String getTargetName()
    {
        return targetName;
    }

    protected void setTargetName( String targetName )
    {
        this.targetName = targetName;
    }

    public File getProjectFile() 
    {
        return projectFile;
    }

    public File getBaseDirectory() 
    {
        return baseDirectory;
    }
    
    protected void setBaseDirectory( File baseDirectory )
    {
        this.baseDirectory = baseDirectory;
    }
    
    public String getConfiguration() 
    {
        return configuration;
    }
    
    protected void setConfiguration( String configuration ) 
    {
        this.configuration = configuration;
    }
    
    public String getPlatform() 
    {
        return platform;
    }
    
    protected void setPlatform( String platform ) 
    {
        this.platform = platform;
    }

    /**
     * Get the value of the configured 'Output Directory'
     * @return the string value
     */
    public File getOutputDirectory()
    {
        return outputDirectory;
    }
    
    @Override
    public String toString()
    { 
        return name + "-" + platform + "-" + configuration;
    }

    /**
     * Set the stored value for outDir
     * @param outputDirectory the new value
     */
    protected void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public List<File> getIncludeDirectories() 
    {
        return includeDirectories;
    }

    protected void setIncludeDirectories( List<File> includeDirectories ) 
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
    private String targetName;
    private File projectFile;
    /**
     * The directory where the solution for this project lives, typically one level up from the project file.
     * If this project is 'standalone' i.e. has no solution this will be the directory that the project file is in.
     */
    private File baseDirectory;
    private String configuration;
    private String platform;
    private File outputDirectory;
    private List<File> includeDirectories;
    private List<String> preprocessorDefs;
}

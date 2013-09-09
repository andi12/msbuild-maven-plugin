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

package uk.org.raje.maven.plugin.msbuild.citools;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import uk.org.raje.maven.plugin.msbuild.configuration.BuildPlatform;

/**
 * Abstract base class for MSBuild Mojos.
 */
public abstract class AbstractCIToolsMojo extends AbstractMojo
{
    /**
     * The file extension for solution files.
     */
    public static final String SOLUTION_EXTENSION = "sln";
    
    /**
     * Is the configured project a solution
     * @return true if the project file name configured ends '.sln'
     */
    protected boolean isSolution()
    {
        boolean result = false;
        
        if ( ( projectFile != null ) 
                && ( projectFile.getName().toLowerCase().endsWith( "." + SOLUTION_EXTENSION ) ) )
        {
            result = true;
        }
        return result;
    }    

    /**
     * The MavenProject for the current build.
     */
    @Parameter( defaultValue = "${project}" )
    protected MavenProject mavenProject;

    /**
     * The project or solution file to build.
     */
    @Parameter( readonly = false, required = true )
    protected File projectFile;

    /**
     * The set of platforms to build.
     */
    @Parameter( readonly = false,  required = false )
    protected List<BuildPlatform> platforms;
}

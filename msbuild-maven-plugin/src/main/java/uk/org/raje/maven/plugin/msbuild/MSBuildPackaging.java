/*
 * Copyright 2013 Andrew Everitt, Andrew Heckford, Daniele Daniele
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
package uk.org.raje.maven.plugin.msbuild;

/**
 * Constants and validation for MSBuild packaging types.
 */
public class MSBuildPackaging 
{
    /**
     * Windows static library
     */
    public static final String LIB = "lib";
    
    /**
     * Windows dynamic library
     */
    public static final String DLL = "dll";
    
    /**
     * Windows application
     */
    public static final String EXE = "exe";
    
    // Don't allow instances of this class
    private MSBuildPackaging() 
    {
    }

    /**
     * Test whether a packaging is a valid MSBuild packaging
     * @param packaging string to test
     * @return true if the packaging is valid for MSBuild
     */
    public static boolean isValid( String packaging )
    {
        return EXE.equals( packaging ) || LIB.equals( packaging ) || DLL.equals( packaging );
    }

    /**
     * Return a string listing the valid packaging types
     * @return a common separated list of packaging types
     */
    public static final String validPackaging()
    {
        return new StringBuilder()
            .append( LIB ).append( ", " )
            .append( DLL ).append( ", " )
            .append( EXE ).toString();
    }
}

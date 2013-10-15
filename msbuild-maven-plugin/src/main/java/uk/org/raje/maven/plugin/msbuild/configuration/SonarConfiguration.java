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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugins.annotations.Parameter;


/**
 * Configuration holder for CppCheck configuration values.
 */
public class SonarConfiguration
{

    /**
     * The CppCheck name to output on debug/information messages
     */    
    public static final String SONAR_NAME = "Sonar";
    
    /**
     * Get the configured value for skip 
     * @return the configured value or false if not configured
     */
    public final boolean skip()
    {
        return skip;
    }
    
    /**
     * Get the list of suffixes that identify source code files
     * @return the list of suffixes source code files 
     */
    public final List<String> getSourceSuffixes()
    {
        return sourceSuffixes;
    }

    /**
     * Get the list of suffixes that identify header files
     * @return the list of suffixes header files 
     */
    public final List<String> getHeaderSuffixes()
    {
        return headerSuffixes;
    }

    /**
     * Get the list of file patterns that Sonar should exclude from the analysis 
     * @return the list of file patterns that Sonar should exclude from the analysis
     */
    public final List<String> getExcludes()
    {
        return excludes;
    }
    
    /**
     * Get the list of preprocessor definitions that Sonar must use when analysing C++ code. 
     * @return the list of preprocessor definitions SOnar must use when analysing C++ code.
     */
    public final List<String> getPreprocessorDefs()
    {
        return preprocessorDefs;
    }

    /**
     * Get the configured exclusion regex, may be null.
     * @return the configured String or null
     */
    public final String getExcludeProjectRegex()
    {
        return excludeProjectRegex;
    }

    /**
     * Set to true to skip Sonar functionality.
     */
    @Parameter( 
            defaultValue = "true", 
            required = false,
            readonly = false )
    private boolean skip = true; 

    /**
     * The list of suffixes identifying the source code files that Sonar will parse
     */
    @Parameter( 
            defaultValue = ".cxx, .cpp, .cc, .c",
            required = false,
            readonly = false )
    private List<String> sourceSuffixes = new LinkedList<String>( Arrays.asList( ".cxx", ".cpp", ".cc", ".c" ) ); 

    /**
     * The list of suffixes identifying the header files that Sonar will parse
     */
    @Parameter( 
            defaultValue = ".hxx, .hpp, .hh, .h",
            required = false,
            readonly = false )
    private List<String> headerSuffixes = new LinkedList<String>( Arrays.asList( ".hxx", ".hpp", ".hh", ".h" ) ); 
    
    /**
     * The list file patterns that Sonar should exclude from analysis
     */
    @Parameter( 
            required = false,
            readonly = false )
    private List<String> excludes = new LinkedList<String>(); 
    
    /** 
     * The code parser in Sonar cannot cope with some macros (for example, MFC macros, va_start, va_arg, va_end), so 
     * they can be redefined using this property (for example, ["va_start(x) 0", "va_arg(x) 0", "va_end(x) 0"])     
     */
    @Parameter( 
            defaultValue = "va_start(x) 0, va_arg(x) 0, va_arg(x, y) 0, va_end(x) 0, _declspec(x), __pragma(x), " 
                    + "__stdcall, DECLARE_MESSAGE_MAP(), DECLARE_DYNCREATE(x), DECLARE_DYNAMIC(x), DECLARE_HANDLE(x)",
            required = false,
            readonly = false )
    private List<String> preprocessorDefs = Arrays.asList( 
            "va_start(x) 0", 
            "va_arg(x) 0", 
            "va_arg(x\\, y) 0",
            "va_end(x) 0", 
            "__declspec(x)", 
            "__pragma(x)", 
            "__stdcall", 
            "DECLARE_MESSAGE_MAP()", 
            "DECLARE_DYNCREATE(x)", 
            "DECLARE_DYNAMIC(x)", 
            "DECLARE_HANDLE(x)" ); 

    /**
     * A regular expression that matches project names that should be excluded from Sonar analysis.
     */
    @Parameter( 
            readonly = false, 
            required = false )
    private String excludeProjectRegex;
}

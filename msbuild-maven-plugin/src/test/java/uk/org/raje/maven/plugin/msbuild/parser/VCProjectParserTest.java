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
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Test CppCheckMojo configuration options.
 */
public class VCProjectParserTest
{
    @Test
    public void testSinglePlatformSingleConfigProprocessorDefs()
    {
        testProject( TEST_SOLUTION_PREPROCESSOR_DEFS, TEST_PLATFORMS[0], TEST_CONFIGURATIONS[0], 
                TEST_PREPROCESSOR_DEFS[0][0], new String[0] );
    }

    @Test
    public void testSinglePlatformMultipleConfigProprocessorDefs()
    {
        for ( int i = 0; i < TEST_CONFIGURATIONS.length; i++ )
        {
            testProject( TEST_SOLUTION_PREPROCESSOR_DEFS, TEST_PLATFORMS[0], TEST_CONFIGURATIONS[i], 
                    TEST_PREPROCESSOR_DEFS[0][i], new String[0] );
        }
    }

    @Test
    public void testMultiplePlatformSingleConfigProprocessorDefs()
    {
        for ( int i = 0; i < TEST_PLATFORMS.length; i++ )
        {
            testProject( TEST_SOLUTION_PREPROCESSOR_DEFS, TEST_PLATFORMS[i], TEST_CONFIGURATIONS[0], 
                    TEST_PREPROCESSOR_DEFS[i][0], new String[0] );
        }
    }
    
    @Test
    public void testSinglePlatformSingleConfigIncludeDirs()
    {
        testProject( TEST_SOLUTION_INCLUDE_DIRS, TEST_PLATFORMS[0], TEST_CONFIGURATIONS[0], 
                new String[0], TEST_INCLUDE_DIRS[0][0] );
    }

    @Test
    public void testSinglePlatformMultipleConfigIncludeDirs()
    {
        for ( int i = 0; i < TEST_CONFIGURATIONS.length; i++ )
        {
            testProject( TEST_SOLUTION_INCLUDE_DIRS, TEST_PLATFORMS[0], TEST_CONFIGURATIONS[i], 
                    new String[0], TEST_INCLUDE_DIRS[0][i] );
        }
    }

    @Test
    public void testMultiplePlatformSingleConfigIncludeDirs()
    {
        for ( int i = 0; i < TEST_PLATFORMS.length; i++ )
        {
            testProject( TEST_SOLUTION_INCLUDE_DIRS, TEST_PLATFORMS[i], TEST_CONFIGURATIONS[0], 
                    new String[0], TEST_INCLUDE_DIRS[i][0] );
        }
    }    
    
    private void testProject( String projectPath, String platform, String configuration, String[] preprocessorDefs, 
            String[] includeDirs )
    {
        File projectFile = new File( this.getClass().getResource( projectPath ).getPath() );
        VCProject vcProject = parseProject( projectFile, platform, configuration ); 

        Assert.assertEquals( Arrays.asList( preprocessorDefs ), vcProject.getPreprocessorDefs() );
        Assert.assertEquals( Arrays.asList( includeDirs ), vcProject.getIncludeDirectories() );
    }

    private VCProject parseProject( File projectFile, String platform, String configuration )
    {
        VCProjectParser projectParser = null;
        
        try 
        {
            projectParser = new VCProjectParser( projectFile, platform, configuration );
        } 
        catch ( FileNotFoundException | SAXException | ParserConfigurationException err ) 
        {
            Assert.fail( err.getMessage() );
        }
        
        try 
        {
            projectParser.parse();
        } 
        catch ( IOException | ParseException err ) 
        {
            Assert.fail( err.getMessage() );
        }
        
        VCProject vcProject = new VCProject( TEST_PROJECT_NAMES[0], projectFile );
        projectParser.updateVCProject( vcProject );
        
        return vcProject;
    }
    
    private static final String TEST_RESOURCE_DIR = "/unit/cppcheck/";

    private static final String TEST_SOLUTION_PREPROCESSOR_DEFS = TEST_RESOURCE_DIR
            + "hello-world-solution-preprocesor-defs/hello-world-project/hello-world.vcxproj";    

    private static final String TEST_SOLUTION_INCLUDE_DIRS = TEST_RESOURCE_DIR
            + "hello-world-solution-include-dirs/hello-world-project/hello-world.vcxproj";    

    
    private static final String[] TEST_PROJECT_NAMES = { "hello-world", "goodbye-world" };
    private static final String[] TEST_PLATFORMS = { "Win32", "x64" };
    private static final String[] TEST_CONFIGURATIONS = { "Debug", "Release" };
    
    private static final String[][][] TEST_PREPROCESSOR_DEFS = 
    {  
        {                                               //Win32
            { "WIN32", "_DEBUG", "_CONSOLE" },          //  Debug
            { "WIN32", "NDEBUG", "_CONSOLE" }           //  Release
        },
        {                                               //x64
            { "_WIN64", "_DEBUG", "_CONSOLE" },          //  Debug
            { "_WIN64", "NDEBUG", "_CONSOLE" }           //  Release
        },
    };

    private static final String[][][] TEST_INCLUDE_DIRS = 
    {  
        {                                               //Win32
            { "test-include-win32-debug" },             //  Debug
            { "test-include-win32-release" }            //  Release
        },
        {                                               //x64
            { "test-include-win64-debug" },             //  Debug
            { "test-include-win64-release" }            //  Release
        },
    };
    
}

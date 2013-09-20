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

import static org.junit.Assert.assertEquals;

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
        testProject( TEST_PROJECT_PREPROCESSOR_DEFS, TEST_PLATFORMS[0], TEST_CONFIGURATIONS[0], 
                TEST_PREPROCESSOR_DEFS[0][0], new String[0] );
    }

    @Test
    public void testSinglePlatformMultipleConfigProprocessorDefs()
    {
        for ( int i = 0; i < TEST_CONFIGURATIONS.length; i++ )
        {
            testProject( TEST_PROJECT_PREPROCESSOR_DEFS, TEST_PLATFORMS[0], TEST_CONFIGURATIONS[i], 
                    TEST_PREPROCESSOR_DEFS[0][i], new String[0] );
        }
    }

    @Test
    public void testMultiplePlatformSingleConfigProprocessorDefs()
    {
        for ( int i = 0; i < TEST_PLATFORMS.length; i++ )
        {
            testProject( TEST_PROJECT_PREPROCESSOR_DEFS, TEST_PLATFORMS[i], TEST_CONFIGURATIONS[0], 
                    TEST_PREPROCESSOR_DEFS[i][0], new String[0] );
        }
    }
    
    @Test
    public void testSinglePlatformSingleConfigIncludeDirs()
    {
        testProject( TEST_PROJECT_INCLUDE_DIRS, TEST_PLATFORMS[0], TEST_CONFIGURATIONS[0], 
                new String[0], TEST_INCLUDE_DIRS[0][0] );
    }

    @Test
    public void testSinglePlatformMultipleConfigIncludeDirs()
    {
        for ( int i = 0; i < TEST_CONFIGURATIONS.length; i++ )
        {
            testProject( TEST_PROJECT_INCLUDE_DIRS, TEST_PLATFORMS[0], TEST_CONFIGURATIONS[i], 
                    new String[0], TEST_INCLUDE_DIRS[0][i] );
        }
    }

    @Test
    public void testMultiplePlatformSingleConfigIncludeDirs()
    {
        for ( int i = 0; i < TEST_PLATFORMS.length; i++ )
        {
            testProject( TEST_PROJECT_INCLUDE_DIRS, TEST_PLATFORMS[i], TEST_CONFIGURATIONS[0], 
                    new String[0], TEST_INCLUDE_DIRS[i][0] );
        }
    }    

    @Test
    public void testEmptyConfigSetting()
    {
        for ( int i = 0; i < TEST_CONFIGURATIONS.length; i++ )
        {
            testProject( TEST_PROJECT_EMPTY_SETTINGS, TEST_PLATFORMS[0], TEST_CONFIGURATIONS[i], 
                    new String[0], new String[0] );
        }
    }    

    @Test
    public void testOutDefaultDir()
    {
        for ( int i = 0; i < TEST_CONFIGURATIONS.length; i++ )
        {
            VCProject vcProject = testProject( TEST_PROJECT_OUTDIR_DEFAULT, TEST_PLATFORMS[0], TEST_CONFIGURATIONS[i], 
                    TEST_PREPROCESSOR_DEFS[0][i], new String[0] );
            assertEquals( new File( vcProject.getPath().getParent(), TEST_CONFIGURATIONS[i] ),
                    vcProject.getOutDir() );
        }
    }

    @Test
    public void testOutDir()
    {
        for ( int i = 0; i < TEST_CONFIGURATIONS.length; i++ )
        {
            VCProject vcProject = testProject( TEST_PROJECT_OUTDIR_SET, TEST_PLATFORMS[0], TEST_CONFIGURATIONS[i], 
                    TEST_PREPROCESSOR_DEFS[0][i], new String[0] );
            assertEquals( new File( vcProject.getPath().getParentFile(), "Runtime\\" + TEST_CONFIGURATIONS[i] ),
                    vcProject.getOutDir() );
        }
    }
    
    private VCProject testProject( String projectPath, String platform, String configuration, 
            String[] preprocessorDefs, String[] includeDirs )
    {
        File projectFile = new File( this.getClass().getResource( projectPath ).getPath() );
        VCProject vcProject = parseProject( projectFile, platform, configuration ); 

        assertEquals( Arrays.asList( preprocessorDefs ), vcProject.getPreprocessorDefs() );
        assertEquals( Arrays.asList( includeDirs ), vcProject.getIncludeDirectories() );
        
        return vcProject;
    }

    private VCProject parseProject( File projectFile, String platform, String configuration )
    {
        VCProjectParser projectParser = null;
        
        try 
        {
            projectParser = new VCProjectParser( projectFile, platform, configuration );
        } 
        catch ( FileNotFoundException fnfe ) 
        {
            Assert.fail( fnfe.getMessage() );
        }
        catch ( SAXException se ) 
        {
            Assert.fail( se.getMessage() );
        }
        catch ( ParserConfigurationException pce )
        {
            Assert.fail( pce.getMessage() );
        }
        
        try 
        {
            projectParser.parse();
        } 
        catch ( IOException ioe ) 
        {
            Assert.fail( ioe.getMessage() );
        }
        catch ( ParseException pe ) 
        {
            Assert.fail( pe.getMessage() );
        }
        
        VCProject vcProject = new VCProject( TEST_PROJECT_NAMES[0], projectFile );
        vcProject.setPlatform( platform );
        vcProject.setConfiguration( configuration );
        projectParser.updateVCProject( vcProject );
        
        return vcProject;
    }
    
    private static final String CONFIG_TEST_RESOURCE_DIR = "/unit/configurations/";
    private static final String CPPCHECK_TEST_RESOURCE_DIR = "/unit/cppcheck/";

    private static final String TEST_PROJECT_PREPROCESSOR_DEFS = CPPCHECK_TEST_RESOURCE_DIR
            + "hello-world-project-preprocesor-defs/hello-world-app.vcxproj";    

    private static final String TEST_PROJECT_INCLUDE_DIRS = CPPCHECK_TEST_RESOURCE_DIR
            + "hello-world-project-include-dirs/hello-world-app.vcxproj";    

    private static final String TEST_PROJECT_EMPTY_SETTINGS = CPPCHECK_TEST_RESOURCE_DIR
            + "hello-world-project-empty-settings/hello-world-makefile.vcxproj";    

    private static final String TEST_PROJECT_OUTDIR_DEFAULT = CONFIG_TEST_RESOURCE_DIR
            + "configurations-test.vcxproj";    

    private static final String TEST_PROJECT_OUTDIR_SET = CONFIG_TEST_RESOURCE_DIR
            + "configurations-outdir-test.vcxproj";    
    
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

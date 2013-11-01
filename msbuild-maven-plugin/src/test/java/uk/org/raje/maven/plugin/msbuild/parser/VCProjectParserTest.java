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
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Test CppCheckMojo configuration options.
 */
public class VCProjectParserTest
{
    @Test
    public void testSinglePlatformSingleConfigProprocessorDefs()
            throws ParserConfigurationException, ParseException, SAXException, IOException
    {
        final File projectFile = getResourceFile( TEST_PROJECT_PREPROCESSOR_DEFS );
        VCProject expectedVCProject = new VCProject( TEST_PROJECT_NAMES[0], projectFile, TEST_PLATFORMS[WIN32], 
                TEST_CONFIGURATIONS[RELEASE] );
        
        expectedVCProject.setPreprocessorDefs( Arrays.asList( TEST_PREPROCESSOR_DEFS[WIN32][RELEASE] ) );
        expectedVCProject.setOutputDirectory( getOutputDirectory( projectFile, WIN32, RELEASE ) );
        testProject( expectedVCProject, null );
    }

    @Test
    public void testSinglePlatformMultipleConfigProprocessorDefs()
            throws ParserConfigurationException, ParseException, SAXException, IOException
    {
        final File projectFile = getResourceFile( TEST_PROJECT_PREPROCESSOR_DEFS );
        
        for ( int c = 0; c < TEST_CONFIGURATIONS.length; c++ )
        {
            VCProject expectedVCProject = 
                    new VCProject( TEST_PROJECT_NAMES[0], projectFile, TEST_PLATFORMS[WIN32], TEST_CONFIGURATIONS[c] );
            
            expectedVCProject.setPreprocessorDefs( Arrays.asList( TEST_PREPROCESSOR_DEFS[WIN32][c] ) );
            expectedVCProject.setOutputDirectory( getOutputDirectory( projectFile, WIN32, c ) );
            testProject( expectedVCProject, null );
        }
    }

    @Test
    public void testMultiplePlatformSingleConfigProprocessorDefs()
            throws ParserConfigurationException, ParseException, SAXException, IOException
    {
        final File projectFile = getResourceFile( TEST_PROJECT_PREPROCESSOR_DEFS );

        for ( int p = 0; p < TEST_PLATFORMS.length; p++ )
        {
            VCProject expectedVCProject = 
                    new VCProject( TEST_PROJECT_NAMES[0], projectFile, TEST_PLATFORMS[p], TEST_CONFIGURATIONS[DEBUG] );
            
            expectedVCProject.setPreprocessorDefs( Arrays.asList( TEST_PREPROCESSOR_DEFS[p][DEBUG] ) );
            expectedVCProject.setOutputDirectory( getOutputDirectory( projectFile, p, DEBUG ) );
            testProject( expectedVCProject, null );
        }
    }
    
    @Test
    public void testSinglePlatformSingleConfigIncludeDirs()
            throws ParserConfigurationException, ParseException, SAXException, IOException
    {
        final File projectFile = getResourceFile( TEST_PROJECT_INCLUDE_DIRS );
        VCProject expectedVCProject = new VCProject( TEST_PROJECT_NAMES[0], projectFile, TEST_PLATFORMS[WIN32], 
                TEST_CONFIGURATIONS[RELEASE] );
        
        expectedVCProject.setIncludeDirectories( Arrays.asList( TEST_INCLUDE_DIRS[WIN32][RELEASE] ) );
        expectedVCProject.setOutputDirectory( getOutputDirectory( projectFile, WIN32, RELEASE ) );
        testProject( expectedVCProject, null );
    }

    @Test
    public void testSinglePlatformMultipleConfigIncludeDirs()
            throws ParserConfigurationException, ParseException, SAXException, IOException
    {
        final File projectFile = getResourceFile( TEST_PROJECT_INCLUDE_DIRS );

        for ( int c = 0; c < TEST_CONFIGURATIONS.length; c++ )
        {
            VCProject expectedVCProject = 
                    new VCProject( TEST_PROJECT_NAMES[0], projectFile, TEST_PLATFORMS[WIN32], TEST_CONFIGURATIONS[c] );
            
            expectedVCProject.setIncludeDirectories( Arrays.asList( TEST_INCLUDE_DIRS[WIN32][c] ) );
            expectedVCProject.setOutputDirectory( getOutputDirectory( projectFile, WIN32, c ) );
            testProject( expectedVCProject, null );
        }
    }

    @Test
    public void testMultiplePlatformSingleConfigIncludeDirs()
            throws ParserConfigurationException, ParseException, SAXException, IOException
    {
        final File projectFile = getResourceFile( TEST_PROJECT_INCLUDE_DIRS );
        
        for ( int p = 0; p < TEST_PLATFORMS.length; p++ )
        {
            VCProject expectedVCProject = 
                    new VCProject( TEST_PROJECT_NAMES[0], projectFile, TEST_PLATFORMS[p], TEST_CONFIGURATIONS[DEBUG] );
            
            expectedVCProject.setIncludeDirectories( Arrays.asList( TEST_INCLUDE_DIRS[p][DEBUG] ) );
            expectedVCProject.setOutputDirectory( getOutputDirectory( projectFile, p, DEBUG ) );
            testProject( expectedVCProject, null );
        }
    }    

    @Test
    public void testEmptyConfigSetting()
            throws ParserConfigurationException, ParseException, SAXException, IOException
    {
        final File projectFile = getResourceFile( TEST_PROJECT_EMPTY_SETTINGS );

        for ( int c = 0; c < TEST_CONFIGURATIONS.length; c++ )
        {
            VCProject expectedVCProject = 
                    new VCProject( TEST_PROJECT_NAMES[0], projectFile, TEST_PLATFORMS[WIN32], TEST_CONFIGURATIONS[c] );
            
            expectedVCProject.setOutputDirectory( getOutputDirectory( projectFile, WIN32, c ) );
            testProject( expectedVCProject, null );
        }
    }    

    @Test
    public void testDefaultOutputDir()
            throws ParserConfigurationException, ParseException, SAXException, IOException
    {
        final File projectFile = getResourceFile( TEST_PROJECT_DEFAULT_OUTDIR );

        for ( int c = 0; c < TEST_CONFIGURATIONS.length; c++ )
        {
            VCProject expectedVCProject = 
                    new VCProject( TEST_PROJECT_NAMES[0], projectFile, TEST_PLATFORMS[WIN32], TEST_CONFIGURATIONS[c] );

            expectedVCProject.setPreprocessorDefs( Arrays.asList( TEST_PREPROCESSOR_DEFS[WIN32][c] ) );
            expectedVCProject.setOutputDirectory( getOutputDirectory( projectFile, WIN32, c ) );
            testProject( expectedVCProject, null );
        }
    }

    @Test
    public void testCustomOutputDirOnProject()
            throws ParserConfigurationException, ParseException, SAXException, IOException
    {
        final File projectFile = getResourceFile( TEST_PROJECT_CUSTOM_OUTDIR );

        for ( int c = 0; c < TEST_CONFIGURATIONS.length; c++ )
        {
            VCProject expectedVCProject = 
                    new VCProject( TEST_PROJECT_NAMES[0], projectFile, TEST_PLATFORMS[WIN32], TEST_CONFIGURATIONS[c] );

            expectedVCProject.setPreprocessorDefs( Arrays.asList( TEST_PREPROCESSOR_DEFS[WIN32][c] ) );
            expectedVCProject.setOutputDirectory( new File( projectFile.getParentFile(), 
                    "Runtime" + File.separator + TEST_CONFIGURATIONS[c] ) );
            
            testProject( expectedVCProject, null );
        }
    }

    /**
     * Test OutDir with older style layout in project file.
     */
    
    @Test
    public void testCustomOutputDir2OnProject()
            throws ParserConfigurationException, ParseException, SAXException, IOException
    {
        final File projectFile = getResourceFile( TEST_PROJECT_CUSTOM_OUTDIR2 );

        for ( int c = 0; c < TEST_CONFIGURATIONS.length; c++ )
        {
            VCProject expectedVCProject = 
                    new VCProject( TEST_PROJECT_NAMES[0], projectFile, TEST_PLATFORMS[WIN32], TEST_CONFIGURATIONS[c] );

            expectedVCProject.setPreprocessorDefs( Arrays.asList( TEST_PREPROCESSOR_DEFS[WIN32][c] ) );
            expectedVCProject.setOutputDirectory( new File( projectFile.getParentFile(), 
                    "Runtime" + File.separator + TEST_PLATFORMS[WIN32] + File.separator + TEST_CONFIGURATIONS[c] ) );
            
            testProject( expectedVCProject, null );
        }
    }

    /**
     * As {@link #testOutDir2()} but including a solution file
     */
    @Test
    public void testCustomOutputDir2OnSolution()
            throws ParserConfigurationException, ParseException, SAXException, IOException
    {
        final File projectFile = getResourceFile( TEST_PROJECT_CUSTOM_OUTDIR2 );

        for ( int c = 0; c < TEST_CONFIGURATIONS.length; c++ )
        {
            VCProject expectedVCProject = 
                    new VCProject( TEST_PROJECT_NAMES[0], projectFile, TEST_PLATFORMS[WIN32], TEST_CONFIGURATIONS[c] );

            expectedVCProject.setPreprocessorDefs( Arrays.asList( TEST_PREPROCESSOR_DEFS[WIN32][c] ) );
            expectedVCProject.setOutputDirectory( new File( projectFile.getParentFile().getParentFile(), 
                    "Runtime" + File.separator + TEST_PLATFORMS[WIN32] + File.separator + TEST_CONFIGURATIONS[c] ) );
            
            testProject( expectedVCProject, getResourceFile( TEST_CONFIG_SOLUTION ) );
        }
    }
    
    @Test
    public void testRelativeOutDirOnProject()
            throws ParserConfigurationException, ParseException, SAXException, IOException
    {
        final File projectFile = getResourceFile( TEST_PROJECT_RELATIVE_OUTDIR );

        for ( int c = 0; c < TEST_CONFIGURATIONS.length; c++ )
        {
            VCProject expectedVCProject = 
                    new VCProject( TEST_PROJECT_NAMES[0], projectFile, TEST_PLATFORMS[WIN32], TEST_CONFIGURATIONS[c] );

            expectedVCProject.setPreprocessorDefs( Arrays.asList( TEST_PREPROCESSOR_DEFS[WIN32][c] ) );
            expectedVCProject.setOutputDirectory( new File( projectFile.getParentFile(), 
                    "." + File.separator + TEST_CONFIGURATIONS[c] ) );
            
            testProject( expectedVCProject, null );
        }
    }
    
    @Test
    public void testRelativeOutDirOnSolution()
            throws ParserConfigurationException, ParseException, SAXException, IOException
    {
        final File projectFile = getResourceFile( TEST_PROJECT_RELATIVE_OUTDIR );

        for ( int c = 0; c < TEST_CONFIGURATIONS.length; c++ )
        {
            VCProject expectedVCProject = 
                    new VCProject( TEST_PROJECT_NAMES[0], projectFile, TEST_PLATFORMS[WIN32], TEST_CONFIGURATIONS[c] );

            expectedVCProject.setPreprocessorDefs( Arrays.asList( TEST_PREPROCESSOR_DEFS[WIN32][c] ) );
            expectedVCProject.setOutputDirectory( new File( projectFile.getParentFile(), 
                    "." + File.separator + TEST_CONFIGURATIONS[c] ) );
            
            testProject( expectedVCProject, getResourceFile( TEST_CONFIG_SOLUTION ) );
        }
    }
    
    @Test
    public void testEnvVariableInIncludeDir()
            throws ParserConfigurationException, ParseException, SAXException, IOException
    {
        final File projectFile = getResourceFile( TEST_PROJECT_ENVVARIABLE );
        final HashMap<String, String> envVariables = new HashMap<String, String>();

        envVariables.put( TEST_ENVAVARIABLE_NAME, TEST_DIR.getPath() );
        VCProject expectedVCProject = 
                new VCProject( TEST_PROJECT_NAMES[0], projectFile, TEST_PLATFORMS[WIN32], TEST_CONFIGURATIONS[DEBUG] );

        expectedVCProject.setPreprocessorDefs( Arrays.asList( TEST_PREPROCESSOR_DEFS[WIN32][DEBUG] ) );
        expectedVCProject.setIncludeDirectories( Arrays.asList( TEST_DIR ) );
        expectedVCProject.setOutputDirectory( getOutputDirectory( projectFile, WIN32, DEBUG ) );
        testProject( expectedVCProject, null, envVariables );
    }
    
    @Test
    public void testNoEnvVariableReplacementInIncludeDir()
            throws ParserConfigurationException, ParseException, SAXException, IOException
    {
        final File projectFile = getResourceFile( TEST_PROJECT_ENVVARIABLE );
        final HashMap<String, String> envVariables = new HashMap<String, String>();

        envVariables.put( TEST_ENVAVARIABLE_NAME, TEST_DIR.getPath() );
        VCProject expectedVCProject = new VCProject( TEST_PROJECT_NAMES[0], projectFile, TEST_PLATFORMS[WIN32], 
                TEST_CONFIGURATIONS[RELEASE] );

        expectedVCProject.setPreprocessorDefs( Arrays.asList( TEST_PREPROCESSOR_DEFS[WIN32][RELEASE] ) );
        expectedVCProject.setOutputDirectory( getOutputDirectory( projectFile, WIN32, RELEASE ) );
        expectedVCProject.setIncludeDirectories( Arrays.asList( TEST_DIR, 
                new File( "$(" + TEST_ANOTHER_ENVAVARIABLE_NAME + ")" ) ) );
        
        testProject( expectedVCProject, null, envVariables );
    }    
    
    @Test
    public void testEnvVariableInOutputDirectory()
            throws ParserConfigurationException, ParseException, SAXException, IOException
    {
        final File projectFile = getResourceFile( TEST_PROJECT_ENVVARIABLE );
        final HashMap<String, String> envVariables = new HashMap<String, String>();

        envVariables.put( TEST_ENVAVARIABLE_NAME, TEST_DIR.getPath() );
        VCProject expectedVCProject = 
                new VCProject( TEST_PROJECT_NAMES[0], projectFile, TEST_PLATFORMS[X64], TEST_CONFIGURATIONS[DEBUG] );

        expectedVCProject.setPreprocessorDefs( Arrays.asList( TEST_PREPROCESSOR_DEFS[X64][DEBUG] ) );
        expectedVCProject.setOutputDirectory( TEST_DIR );
        testProject( expectedVCProject, null, envVariables );
    } 
    
    @Test
    public void testEnvVariableInPreprocessorDefs()
            throws ParserConfigurationException, ParseException, SAXException, IOException
    {
        final File projectFile = getResourceFile( TEST_PROJECT_ENVVARIABLE );
        final HashMap<String, String> envVariables = new HashMap<String, String>();

        envVariables.put( TEST_ENVAVARIABLE_NAME, TEST_DIR.getName() );
        VCProject expectedVCProject = 
                new VCProject( TEST_PROJECT_NAMES[0], projectFile, TEST_PLATFORMS[X64], TEST_CONFIGURATIONS[RELEASE] );

        expectedVCProject.setPreprocessorDefs( Arrays.asList( TEST_DIR.getName() ) );
        expectedVCProject.setOutputDirectory( getOutputDirectory( projectFile, X64, RELEASE ) );
        testProject( expectedVCProject, null, envVariables );
    }    
    
    
    private void testProject( VCProject expectedVCProject, File solutionFile )
            throws ParserConfigurationException, ParseException, SAXException, IOException
    {
        testProject( expectedVCProject, solutionFile, Collections.<String, String> emptyMap() );
    }

    private void testProject( VCProject expectedVCProject, File solutionFile, Map<String, String> envVariables )
            throws ParserConfigurationException, ParseException, SAXException, IOException
    {
        VCProject vcTestProject = new VCProject( expectedVCProject.getName(), expectedVCProject.getFile(), 
                expectedVCProject.getPlatform(), expectedVCProject.getConfiguration() );

        VCProjectParser projectParser = new VCProjectParser( vcTestProject.getFile(), solutionFile, 
                vcTestProject.getPlatform(), vcTestProject.getConfiguration() );

        projectParser.setEnvVariables( envVariables );
        projectParser.parse();
        projectParser.updateVCProject( vcTestProject );
        
        assertEquals( expectedVCProject.getPreprocessorDefs(), vcTestProject.getPreprocessorDefs() );
        assertEquals( expectedVCProject.getIncludeDirectories(), vcTestProject.getIncludeDirectories() );
        assertEquals( expectedVCProject.getOutputDirectory(), vcTestProject.getOutputDirectory() );
    }

    private File getResourceFile( String resourcePath )
    {
        return new File( getClass().getResource( resourcePath ).getPath() );
    }
    
    private File getOutputDirectory( File projectFile, int platformId, int configId )
    {
        if ( platformId == X64 )
        {
            return new File( projectFile.getParentFile(), "x64" + File.separator + TEST_CONFIGURATIONS[configId] );
        }
        else
        {
            return new File( projectFile.getParentFile(), TEST_CONFIGURATIONS[configId] );
        }
    }
    
    private static final String CONFIG_TEST_RESOURCE_DIR = "/unit/configurations/";
    private static final String CPPCHECK_TEST_RESOURCE_DIR = "/unit/cppcheck/";

    private static final String TEST_PROJECT_PREPROCESSOR_DEFS = CPPCHECK_TEST_RESOURCE_DIR
            + "hello-world-project-preprocesor-defs/hello-world-app.vcxproj";    

    private static final String TEST_PROJECT_INCLUDE_DIRS = CPPCHECK_TEST_RESOURCE_DIR
            + "hello-world-project-include-dirs/hello-world-app.vcxproj";    

    private static final String TEST_PROJECT_EMPTY_SETTINGS = CPPCHECK_TEST_RESOURCE_DIR
            + "hello-world-project-empty-settings/hello-world-makefile.vcxproj";    

    private static final String TEST_PROJECT_DEFAULT_OUTDIR = CONFIG_TEST_RESOURCE_DIR
            + "configurations-test.vcxproj";    

    private static final String TEST_PROJECT_CUSTOM_OUTDIR = CONFIG_TEST_RESOURCE_DIR
            + "configurations-project/configurations-outdir-test.vcxproj";    
    
    private static final String TEST_PROJECT_CUSTOM_OUTDIR2 = CONFIG_TEST_RESOURCE_DIR
            + "configurations-project/configurations-outdir2-test.vcxproj";    
    
    private static final String TEST_PROJECT_RELATIVE_OUTDIR = CONFIG_TEST_RESOURCE_DIR
            + "configurations-project/configurations-relative-outdir-test.vcxproj";    

    private static final String TEST_PROJECT_ENVVARIABLE = CPPCHECK_TEST_RESOURCE_DIR
            + "hello-world-env-variables/hello-world-app.vcxproj";    

    private static final String TEST_CONFIG_SOLUTION = "/unit/configurations/configurations-test.sln";    
    
    private static final String[] TEST_PROJECT_NAMES = { "hello-world", "goodbye-world" };
    private static final String[] TEST_PLATFORMS = { "Win32", "x64" };
    private static final String[] TEST_CONFIGURATIONS = { "Debug", "Release" };
    private static final String TEST_ENVAVARIABLE_NAME = "TestEnvVariable";
    private static final String TEST_ANOTHER_ENVAVARIABLE_NAME = "AnotherTestEnvVariable";
    
    private static final int WIN32 = 0;
    private static final int X64 = 1;
    private static final int DEBUG = 0;
    private static final int RELEASE = 1;

    private static final String[][][] TEST_PREPROCESSOR_DEFS = 
    {  
        {                                                   //Win32
            { "WIN32", "_DEBUG", "_CONSOLE" },              //  Debug
            { "WIN32", "NDEBUG", "_CONSOLE" }               //  Release
        },
        {                                                   //x64
            { "_WIN64", "_DEBUG", "_CONSOLE" },             //  Debug
            { "_WIN64", "NDEBUG", "_CONSOLE" }              //  Release
        },
    };

    private static final File[][][] TEST_INCLUDE_DIRS = 
    {  
        {                                                   //Win32
            { new File( "test-include-win32-debug" ) },     //  Debug
            { new File( "test-include-win32-release" ) }    //  Release
        },
        {                                                   //x64
            { new File( "test-include-win64-debug" ) },     //  Debug
            { new File( "test-include-win64-release" ) }    //  Release
        },
    };
    
    private static final File TEST_DIR = new File( "TestDir" ).getAbsoluteFile();
    
}

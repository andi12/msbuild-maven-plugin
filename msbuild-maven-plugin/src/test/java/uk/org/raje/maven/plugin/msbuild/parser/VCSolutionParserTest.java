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
import java.util.List;

import org.junit.Test;
import org.junit.Assert;

/**
 * Test CppCheckMojo configuration options.
 */
public class VCSolutionParserTest
{
    @Test
    public void testSinglePlatformSingleConfig()
    {
        testSingleProjectSolution( TEST_PLATFORMS[0], TEST_CONFIGURATIONS[0] );
    }

    @Test
    public void testSinglePlatformMultipleConfig()
    {
        for ( String configuration : TEST_CONFIGURATIONS )
        {
            testSingleProjectSolution( TEST_PLATFORMS[0], configuration );
        }
    }

    @Test
    public void testMultiplePlatformSingleConfig()
    {
        for ( String platform : TEST_PLATFORMS )
        {
            testSingleProjectSolution( platform, TEST_CONFIGURATIONS[0] );
        }
    }
    
    @Test
    public void testMultipleProjects()
    {
        for ( String platform : TEST_PLATFORMS )
        {
            for ( String configuration : TEST_CONFIGURATIONS )
            {
                testMultipleProjectSolution( platform, configuration );
            }
        }
    }
    
    private void testSingleProjectSolution( String platform, String configuration )
    {
        String solutionPath = TEST_RESOURCE_DIR + "hello-world-solution/hello-world.sln";
        File solutionFile = new File( this.getClass().getResource( solutionPath ).getPath() );
        List<VCProject> vcProjects = parseSolution( solutionFile, platform, configuration ); 

        Assert.assertEquals( 1, vcProjects.size() );
        validateProject( vcProjects.get( 0 ), TEST_PROJECT_NAMES[0], platform, configuration );
    }
    
    private void testMultipleProjectSolution( String platform, String configuration )
    {
        String solutionPath = TEST_RESOURCE_DIR + "hello-goodbye-world-solution/hello-goodbye-world.sln";
        File solutionFile = new File( this.getClass().getResource( solutionPath ).getPath() );
        List<VCProject> vcProjects = parseSolution( solutionFile, platform, configuration );
        Assert.assertEquals( TEST_PROJECT_NAMES.length, vcProjects.size() );
        
        for ( int i = 0; i < TEST_PROJECT_NAMES.length; i++ ) 
        {
            validateProject( vcProjects.get( i ), TEST_PROJECT_NAMES[i], platform, configuration );
        }
    }

    private List<VCProject> parseSolution( File solutionFile, String platform, String configuration )
    {
        VCSolutionParser solutionParser = null;
        
        try 
        {
            solutionParser = new VCSolutionParser( solutionFile, platform, configuration, null );
        } 
        catch ( FileNotFoundException fnfe ) 
        {
            Assert.fail( fnfe.getMessage() );
        }
        
        try 
        {
            solutionParser.parse();
        } 
        catch ( IOException ioe ) 
        {
            Assert.fail( ioe.getMessage() );
        }
        catch ( ParseException pe ) 
        {
            Assert.fail( pe.getMessage() );
        }
        
        return solutionParser.getVCProjects();
    }
    
    private void validateProject( VCProject vcProject, String name, String platform, String configuration )
    {
        Assert.assertEquals( name, vcProject.getName() );
        Assert.assertEquals( platform, vcProject.getPlatform() );
        Assert.assertEquals( configuration, vcProject.getConfiguration() );
    }
    
    private static final String TEST_RESOURCE_DIR = "/unit/cppcheck/";
    private static final String[] TEST_PROJECT_NAMES = { "hello-world", "goodbye-world" };
    private static final String[] TEST_PLATFORMS = { "Win32", "x64" };
    private static final String[] TEST_CONFIGURATIONS = { "Debug", "Release" };   
}

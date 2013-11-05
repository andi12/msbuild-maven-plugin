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
package uk.org.raje.maven.plugin.msbuild;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;
import org.xml.sax.SAXException;

import uk.org.raje.maven.plugin.msbuild.parser.VCProject;
import uk.org.raje.maven.plugin.msbuild.parser.VCProjectHolder;

/**
 * Test methods in the abstract base class AbstractMSBuildPluginMojo.
 */
public class AbstractMSBuildPluginMojoTest
{
    @Test
    public void getRelativeFileTest() throws Exception
    {
        assertEquals( new File( "baz" ), 
                instance.getRelativeFile( new File( "C:\\foo\\bar\\" ), new File( "C:\\foo\\bar\\baz\\" ) ) );
    }

    @Test( expected = IOException.class )
    public void getRelativeFileErrorTest() throws Exception
    {
        instance.getRelativeFile( new File( "C:\\foo\\bar\\baz\\" ), new File( "C:\\foo\\bar\\" ) );
    }
    
    @Test
    public void getRelativeFileLongerTest() throws Exception
    {
        assertEquals( new File( "baz\\qux" ), 
                instance.getRelativeFile( new File( "C:\\foo\\bar\\" ), new File( "C:\\foo\\bar\\baz\\qux" ) ) );
    }

    @Test
    public void getRelativeFileDotTest() throws Exception
    {
        assertEquals( new File( "." ), 
                instance.getRelativeFile( new File( "C:\\foo\\bar" ), new File( "C:\\foo\\bar" ) ) );
    }

    @Test
    public void getRelativeFileUnixSlashTest() throws Exception
    {
        assertEquals( new File( "baz/qux" ), 
                instance.getRelativeFile( new File( "C:/foo/bar/" ), new File( "C:\\foo\\bar\\baz\\qux" ) ) );
    }

    @Test
    public void getRelativeFileRelativeTest() throws Exception
    {
        assertEquals( new File( "baz\\qux" ), 
                instance.getRelativeFile( new File( ".\\foo\\bar" ), new File( ".\\foo\\bar\\baz\\qux" ) ) );
    }

    @Test
    public void getRelativeFileDotdotTest() throws Exception
    {
        assertEquals( new File( "foo" ), 
                instance.getRelativeFile( new File( ".." ), new File( "..\\foo" ) ) );
    }

    /**
     * Basic test to list sources for a project with no excludes
     * @throws Exception if something goes wrong
     */
    @Test
    public void getProjectSourcesProject1Test() throws Exception
    {
        List<VCProject> projects = parseProjectSourcesProject1();
        assertEquals( 1, projects.size() );
        VCProject project = projects.get( 0 );
        
        assertListEqualsIgnoreOrder( Arrays.asList( new File[]{
                new File( project.getBaseDirectory(), "targetver.h" ),
                new File( project.getBaseDirectory(), "stdafx.h" ),
                new File( project.getBaseDirectory(), "stdafx.cpp" ),
                new File( project.getBaseDirectory(), "project1.cpp" ) } ), 
                instance.getProjectSources( project, true, Collections.<String> emptyList() ) );
    }

    /**
     * Test listing sources for a project with various excludes.
     * We do not seek to test the pattern matching with these tests just verify our path logic works
     * @throws Exception if something goes wrong
     */
    @Test
    public void getProjectSourcesProject1ExcludesTest() throws Exception
    {
        List<VCProject> projects = parseProjectSourcesProject1();
        assertEquals( 1, projects.size() );
        VCProject project = projects.get( 0 );
        List<String> excludes;
        
        excludes = Arrays.asList( "stdafx.cpp" );
        assertListEqualsIgnoreOrder( Arrays.asList( new File[]{
                new File( project.getBaseDirectory(), "targetver.h" ),
                new File( project.getBaseDirectory(), "stdafx.h" ),
                new File( project.getBaseDirectory(), "project1.cpp" ) } ), 
                instance.getProjectSources( project, true, excludes ) );

        excludes = Arrays.asList( "**/stdafx.cpp" );
        assertListEqualsIgnoreOrder( Arrays.asList( new File[]{
                new File( project.getBaseDirectory(), "targetver.h" ),
                new File( project.getBaseDirectory(), "stdafx.h" ),
                new File( project.getBaseDirectory(), "project1.cpp" ) } ), 
                instance.getProjectSources( project, true, excludes ) );

        excludes = Arrays.asList( "*.cpp" );
        assertListEqualsIgnoreOrder( Arrays.asList( new File[]{
                new File( project.getBaseDirectory(), "targetver.h" ),
                new File( project.getBaseDirectory(), "stdafx.h" ) } ), 
                instance.getProjectSources( project, true, excludes ) );
    }

    /**
     * Basic test to list sources for a project within a solution with no excludes
     * @throws Exception if something goes wrong
     */
    @Test
    public void getProjectSourcesSolutionToProject1Test() throws Exception
    {
        List<VCProject> projects = parseProjectSourcesSolution();
        VCProject project = projects.get( 0 ); // should be project1
        
        assertListEqualsIgnoreOrder( Arrays.asList( new File[]{
                new File( project.getBaseDirectory(), "project1\\targetver.h" ),
                new File( project.getBaseDirectory(), "project1\\stdafx.h" ),
                new File( project.getBaseDirectory(), "project1\\stdafx.cpp" ),
                new File( project.getBaseDirectory(), "project1\\project1.cpp" ) } ), 
                instance.getProjectSources( project, true, Collections.<String> emptyList() ) );
    }

    /**
     * Test listing sources for a project within a solution with various excludes.
     * @throws Exception if something goes wrong
     */
    @Test
    public void getProjectSourcesSolutionToProject1ExcludesTest() throws Exception
    {
        List<VCProject> projects = parseProjectSourcesSolution();
        VCProject project = projects.get( 0 ); // should be project1
        List<String> excludes;
        
        excludes = Arrays.asList( "**/stdafx.cpp" );
        assertListEqualsIgnoreOrder( Arrays.asList( new File[]{
                new File( project.getBaseDirectory(), "project1\\targetver.h" ),
                new File( project.getBaseDirectory(), "project1\\stdafx.h" ),
                new File( project.getBaseDirectory(), "project1\\project1.cpp" ) } ), 
                instance.getProjectSources( project, true, excludes ) );

        excludes = Arrays.asList( "**\\*.cpp" );
        assertListEqualsIgnoreOrder( Arrays.asList( new File[]{
                new File( project.getBaseDirectory(), "project1\\targetver.h" ),
                new File( project.getBaseDirectory(), "project1\\stdafx.h" ) } ), 
                instance.getProjectSources( project, true, excludes ) );

        excludes = Arrays.asList( "project1/stdafx.cpp" );
        assertListEqualsIgnoreOrder( Arrays.asList( new File[]{
                new File( project.getBaseDirectory(), "project1\\targetver.h" ),
                new File( project.getBaseDirectory(), "project1\\stdafx.h" ),
                new File( project.getBaseDirectory(), "project1\\project1.cpp" ) } ), 
                instance.getProjectSources( project, true, excludes ) );

        excludes = Arrays.asList( "project1\\stdafx.cpp" );
        assertListEqualsIgnoreOrder( Arrays.asList( new File[]{
                new File( project.getBaseDirectory(), "project1\\targetver.h" ),
                new File( project.getBaseDirectory(), "project1\\stdafx.h" ),
                new File( project.getBaseDirectory(), "project1\\project1.cpp" ) } ), 
                instance.getProjectSources( project, true, excludes ) );

        excludes = Arrays.asList( "project2/stdafx.cpp" );
        assertListEqualsIgnoreOrder( Arrays.asList( new File[]{
                new File( project.getBaseDirectory(), "project1\\targetver.h" ),
                new File( project.getBaseDirectory(), "project1\\stdafx.h" ),
                new File( project.getBaseDirectory(), "project1\\stdafx.cpp" ),
                new File( project.getBaseDirectory(), "project1\\project1.cpp" ) } ), 
                instance.getProjectSources( project, true, excludes ) );
    }

    @Test
    public void getProjectSourcesProject2Test() throws Exception
    {
        List<VCProject> projects = parseProjectSourcesSolution();
        VCProject project = projects.get( 1 ); // should be project2
        
        assertListEqualsIgnoreOrder( Arrays.asList( new File[]{
                new File( project.getBaseDirectory(), "project2\\project2.h" ),
                new File( project.getBaseDirectory(), "project2\\project2.cpp" ) } ), 
                instance.getProjectSources( project, true, Collections.<String> emptyList() ) );
    }

    private <E> void assertListEqualsIgnoreOrder( List<E> expected, List<E> actual )
    {
        assertEquals( "Lists not the same size", expected.size(), actual.size() );
        for ( E item : expected )
        {
            assertTrue( "Missing " + expected.toString(), actual.contains( item ) );
        }
    }

    private List<VCProject> parseProjectSourcesSolution()
            throws ParseException, SAXException, ParserConfigurationException, IOException
    {
        File projectFile = new File( getClass().getResource( SOURCE_LISTING_SOLUTION_PATH ).getPath() );
        VCProjectHolder vcProjectHolder = VCProjectHolder.getVCProjectHolder( projectFile, true, null );
        return vcProjectHolder.getParsedProjects( "Win32", "Debug" );
    }

    private List<VCProject> parseProjectSourcesProject1()
            throws ParseException, SAXException, ParserConfigurationException, IOException
    {
        File projectFile = new File( getClass().getResource( SOURCE_LISTING_PROJECT1_PATH ).getPath() );
        VCProjectHolder vcProjectHolder = VCProjectHolder.getVCProjectHolder( projectFile, false, null );
        return vcProjectHolder.getParsedProjects( "Win32", "Debug" );
    }

    private static final String SOURCE_LISTING_SOLUTION_PATH = "/unit/source-listing/solution.sln";
    private static final String SOURCE_LISTING_PROJECT1_PATH = "/unit/source-listing/project1/project1.vcxproj";

    /**
     * Basic concrete class for AbstractMSBuildPluginMojo to facilitate testing individual methods.
     */
    private class ConcretetMSBuildPluginMojo extends AbstractMSBuildPluginMojo
    {
        public ConcretetMSBuildPluginMojo()
        {
            setLog( new DefaultLog( new ConsoleLogger() ) );
        }

        @Override
        protected void doExecute() throws MojoExecutionException,
                MojoFailureException 
        {
            throw new MojoExecutionException( "I'm only a crash test dummy" );
        }
    }
    
    private ConcretetMSBuildPluginMojo instance = new ConcretetMSBuildPluginMojo();
}

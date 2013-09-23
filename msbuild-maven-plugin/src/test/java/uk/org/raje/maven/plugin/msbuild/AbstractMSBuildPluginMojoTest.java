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

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;

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

    @Test( expected = MojoExecutionException.class )
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

    private class ConcretetMSBuildPluginMojo extends AbstractMSBuildPluginMojo
    {
        @Override
        protected void doExecute() throws MojoExecutionException,
                MojoFailureException 
        {
            throw new MojoExecutionException( "I'm only a crash test dummy" );
        }
    }
    
    private ConcretetMSBuildPluginMojo instance = new ConcretetMSBuildPluginMojo();
}

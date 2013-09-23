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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

/**
 * Unit tests for the VersionInfoMojo
 */
public class VersionInfoMojoTest extends AbstractMSBuildMojoTestCase
{
    @Test
    public final void testAllSettingsConfiguration() throws Exception 
    {
        VersionInfoMojo viMojo = ( VersionInfoMojo ) lookupConfiguredMojo( VersionInfoMojo.MOJO_NAME, 
                "/unit/configurations/allsettings-pom.xml" );

        assertAllSettingsConfiguration( viMojo );
    }

    @Test
    public final void testMinimalSolutionConfiguration() throws Exception
    {
        
        VersionInfoMojo viMojo = ( VersionInfoMojo ) lookupConfiguredMojo( VersionInfoMojo.MOJO_NAME, 
                "/unit/configurations/minimal-solution-pom.xml" );
        final File rcFile = calculateAndDleteVersionInfoFile( viMojo.mavenProject.getFile().getParentFile() );
        
        viMojo.execute();
        
        assertTrue( "maven-version-info.rc should not be created", ! rcFile.exists() );
    }

    @Test
    public final void testMinimalSolutionWithVersionConfiguration() throws Exception
    {
        VersionInfoMojo viMojo = ( VersionInfoMojo ) lookupConfiguredMojo( VersionInfoMojo.MOJO_NAME, 
                "/unit/configurations/minimal-solution-with-version-pom.xml" );
        final File rcFile = calculateAndDleteVersionInfoFile( viMojo.mavenProject.getFile().getParentFile() );
        
        viMojo.execute();
        
        checkVersionFile( rcFile );
    }


    private File calculateAndDleteVersionInfoFile( File directory )
    {
        final File rcFile = new File( directory, "maven-version-info.rc" );
        rcFile.delete();
        return rcFile;
    }

    private void checkVersionFile( File rcFile ) throws IOException
    {
        BufferedReader br = new BufferedReader( new FileReader( rcFile ) );
        String line;
        while ( ( line = br.readLine() ) != null )
        {
            assertTrue( "Unexpected property marker found in: " + line, ! line.contains( "${" ) );
        }
        br.close();
    }
}

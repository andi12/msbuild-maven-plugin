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
package uk.org.raje.maven.plugin.msbuild.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.maven.it.Verifier;

/**
 * Helper methods for Integration Tests.
 */
class MSBuildMojoITHelper
{
    /**
     * The name of the properties file that contains properties that are used in test POMs 
     * and need to be included by the Verifier.
     */
    private static final String VERIFIER_PROPERTIES_FILE = "/verifier.properties";

    // no instances
    private MSBuildMojoITHelper()
    {
    }

    static void addPropertiesToVerifier( Verifier verifier ) throws IOException
    {
        Properties props = new Properties();
        props.load( MSBuildMojoITHelper.class.getResourceAsStream( VERIFIER_PROPERTIES_FILE ) );
        Properties systemProps = verifier.getSystemProperties();
        systemProps.putAll( props );
        verifier.setSystemProperties( systemProps );
    }

    /**
     * Helper to create the File object for an output directory and make sure
     * that the directory doesn't exist.
     * @param parent abstract pathname of the parent for the output directory
     * @param name the name of the output directory
     * @return an abstract path for the output directory
     * @throws IOException if we are unable to delete files/directories
     */
    static File calculateAndDeleteOutputDirectory( File parent, String name ) throws IOException
    {
        File result = new File( parent, name );
        if ( result.exists() )
        {
            for ( File content : result.listFiles() )
            {
                // NOTE: No attempt to recurse, shouldn't ever be needed!
                if ( !content.delete() )
                {
                    throw new IOException( "Failed to delete " +  content.getAbsolutePath() );
                }
            }
            if ( !result.delete() )
            {
                throw new IOException( "Failed to delete " +  result.getAbsolutePath() );
            }
        }
        
        return result;
    }

    /**
     * Helper to check that the build output is cleaned up by MSBuild
     * @param projectName the project name (needed to verify the xxxClean.log)
     * @param outputDir the directory that should have been cleaned
     */
    static void checkProjectBuildOutputIsCleaned( String projectName, File outputDir )
    {
        if ( outputDir.exists() ) // if the directory doesn't exist we're clean
        {
            List<String> dirContents = Arrays.asList( outputDir.list() );
            if ( dirContents.size() != 0 ) // if the directory contains something it should just be the log file
            {
                assertEquals( 1, dirContents.size() );
                assertTrue( dirContents.contains( projectName + ".Build.CppClean.log" ) );
            }
        }
        
    }
}

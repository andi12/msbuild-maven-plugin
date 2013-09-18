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

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.python.core.PyList;
import org.python.util.PythonInterpreter;


/**
 * Generate a C++ test runner using the CxxTest test framework.
 */
@Mojo( name = CxxTestGenMojo.MOJO_NAME, defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES )
public class CxxTestGenMojo extends AbstractMSBuildPluginMojo 
{
    /**
     * The name this Mojo declares, also represents the goal.
     */
    public static final String MOJO_NAME = "cxxtestgen";
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException 
    {
        if ( !isCxxTestEnabled( "runner generation" ) ) 
        {
            return;
        }
        
        validateCxxTestConfiguration();
        initPythonIntepreter();

        for ( String testTarget : cxxTest.testTargets() ) 
        {
            List<String> arguments = getCxxTestGenArguments( testTarget );
            runCxxTestGen( testTarget, arguments );
        }
    }
    
    private List<String> getCxxTestGenArguments( String testTarget )
    {
        List<String> arguments = new LinkedList<String>();
        File targetPath = new File( projectFile.getParentFile(), testTarget );
        
        arguments.add( "--have-eh" );
        arguments.add( "--abort-on-fail" );
        arguments.add( "--xunit-printer" );
        arguments.add( "--xunit-file=" 
                + new File( targetPath, cxxTest.reportNamePrefix() ).getAbsolutePath().replace( "\\", "/" ) );
        
        arguments.add( "--output=" + new File( targetPath, cxxTest.testRunnerName() ).getAbsolutePath() );
        arguments.add( new File( targetPath, cxxTest.testHeaderPattern() ).getAbsolutePath() );
        
        return arguments;
    }
    
    private void runCxxTestGen( String testTarget, List<String> arguments ) throws MojoExecutionException
    {
        final String cxxTestGenArgVar = "cxxTestGenArgs";
        
        String cxxTestGenPath = cxxTest.cxxTestHome().getAbsolutePath();
        PyList cxxTestGenArgs = new PyList( arguments );
        cxxTestGenArgs.add( 0, cxxTestGenPath );
        
        getLog().info( "Executing test runner generation for target " + testTarget + "." );
        getLog().debug( "Executing Python script " + cxxTestGenPath + " with arguments=" + cxxTestGenArgs );

        PythonInterpreter pythonInterpreter = new PythonInterpreter();
        pythonInterpreter.set( cxxTestGenArgVar, cxxTestGenArgs );
        pythonInterpreter.exec( "from cxxtest import *; main(" + cxxTestGenArgVar + ")" );  
        pythonInterpreter.cleanup();
        
        getLog().info( "Test runner generation for target " + testTarget + " succeeded." );
    }
    
    private void initPythonIntepreter()
    {
        Properties postProperties = new Properties();
        
        getLog().debug( "Initialising Jython with properties=" + postProperties );
        postProperties.put( "python.path", getCxxTestPython2Home().getAbsolutePath() );
        PythonInterpreter.initialize( System.getProperties(), postProperties, null );
    }
}

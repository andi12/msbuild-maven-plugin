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

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import uk.org.raje.maven.plugin.msbuild.configuration.CxxTestConfiguration;

/**
 *  
 */
@Mojo( name = CxxTestBuildMojo.MOJO_NAME, defaultPhase = LifecyclePhase.TEST_COMPILE )
public class CxxTestBuildMojo extends AbstractMSBuildMojo
{
    /**
     * The name this Mojo declares, also represents the goal.
     */
    public static final String MOJO_NAME = "testbuild";

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException
    {
        if ( !isCxxTestEnabled( "runner build" ) )
        {
            return;
        }
        
        validateCxxTestConfiguration();
        validateForMSBuild();

        Map<String, String> environment = new HashMap<String, String>();        
        environment.put( CxxTestConfiguration.CXXTEST_HOME, cxxTest.cxxTestHome().getAbsolutePath() );
        environment.put( "MAVEN_TESTS", "1" );
        
        runMSBuild( cxxTest.testTargets(), environment );
    }
}

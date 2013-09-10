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
package uk.org.raje.maven.plugin.msbuild.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for BuildPlatform configuration holder
 */
public class BuildPlatformTest
{
    /**
     * No arg constructor should create a Win32/Release configuration 
     */
    @Test
    public void noargConstrutorTest()
    {
        BuildPlatform p = new BuildPlatform();
        assertEquals( BuildPlatform.DEFAULT_PLATFORM, p.getName() );
        assertEquals( BuildConfiguration.DEFAULT_CONFIGURATION, p.getConfigurations().get( 0 ).getName() );
    }

    /**
     * Test that constructing with a name creates a &lt;name&gt;/Release configuration
     */
    @Test
    public void nameConstrutorTest()
    {
        BuildPlatform p = new BuildPlatform( "myPlatform" );
        assertEquals( "myPlatform", p.getName() );
        assertEquals( BuildConfiguration.DEFAULT_CONFIGURATION, p.getConfigurations().get( 0 ).getName() );
    }

    /**
     * Test that toString returns the platform name
     */
    @Test
    public void toStringTest()
    {
        BuildPlatform p = new BuildPlatform();
        assertEquals( BuildPlatform.DEFAULT_PLATFORM, p.toString() );
    }

    /**
     * Test that platforms are considered equal based on name
     */
    @Test
    public void simpleEqualsTest()
    {
        BuildPlatform p1 = new BuildPlatform( "one" );
        BuildPlatform p1Again = new BuildPlatform( "one" );
        BuildPlatform p2 = new BuildPlatform( "two" );
        
        assertEquals( p1, p1Again );
        assertTrue( !p1.equals( p2 ) );
    }

}
